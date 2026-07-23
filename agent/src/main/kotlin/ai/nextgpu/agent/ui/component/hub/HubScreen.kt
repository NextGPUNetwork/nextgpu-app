package ai.nextgpu.agent.ui.component.hub

import ai.nextgpu.agent.OllamaStreamingService
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.model.ChatSession
import ai.nextgpu.agent.model.PromptModel
import ai.nextgpu.agent.service.AudioRecorderService
import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.NextGpuAiService
import ai.nextgpu.agent.service.NextGpuVisionService
import ai.nextgpu.agent.service.NextGpuWebService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.AppPortal
import ai.nextgpu.agent.ui.component.hub.sidebar.Sidebar
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.util.OSUtil
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("HubScreen")

/**
 * Retries [block] up to [times] total attempts with a short backoff between attempts,
 * logging every failure (with full stack trace) via [onAttemptFailed] before giving up.
 * Cancellation is never swallowed/retried - it always propagates immediately so
 * coroutine cancellation (e.g. the user hitting "Cancel") behaves correctly.
 */
private suspend fun <T> retrying(
    times: Int = 3,
    initialDelayMs: Long = 1_000,
    backoffFactor: Double = 2.0,
    onAttemptFailed: (attempt: Int, maxAttempts: Int, error: Throwable) -> Unit,
    block: suspend (attempt: Int) -> T,
): T {
    var delayMs = initialDelayMs
    var lastError: Throwable? = null

    for (attempt in 1..times) {
        try {
            return block(attempt)
        } catch (c: CancellationException) {
            throw c
        } catch (t: Throwable) {
            lastError = t
            onAttemptFailed(attempt, times, t)
            if (attempt < times) {
                delay(delayMs)
                delayMs = (delayMs * backoffFactor).toLong()
            }
        }
    }
    throw lastError ?: IllegalStateException("Retry loop exited without a result")
}

object Services {
    val agentService: NextGpuAgentService get() = springContext.getBean(NextGpuAgentService::class.java)
    val aiService: NextGpuAiService get() = springContext.getBean(NextGpuAiService::class.java)
    val visionService: NextGpuVisionService get() = springContext.getBean(NextGpuVisionService::class.java)
    val streamingService: OllamaStreamingService get() = springContext.getBean(OllamaStreamingService::class.java)
    val nextGpuWebService: NextGpuWebService get() = springContext.getBean(NextGpuWebService::class.java)
    val audioRecorderService: AudioRecorderService get() = springContext.getBean(AudioRecorderService::class.java)
}

@Composable
fun HubScreen(
    viewModel: SettingsViewModel,
    onProvider: () -> Unit,
    onLogout: () -> Unit,
    onModels: () -> Unit,
    onSettings: (tabId: String) -> Unit,
    isPrivateMode: Boolean,
    onPrivateModeChange: (Boolean) -> Unit,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
) {
    val agentService = remember { Services.agentService }
    val aiService = remember { Services.aiService }
    val visionService = remember { Services.visionService }
    val streamingService = remember { Services.streamingService }
    val downloadService = remember { springContext.getBean(ModelDownloadService::class.java) }

    var uiState by remember { mutableStateOf(HubUiState()) }
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }



    // UNIQUE IDENTIFIER FOR PRIVATE/TRANSIENT SESSIONS
    // We use this as a fallback key when session.id is null
    var privateSessionTrackingId by remember { mutableStateOf(UUID.randomUUID().toString()) }

    fun getSessionId(session: ChatSession?): String? {
        if (session == null) return null
        return session.id?.toString() ?: privateSessionTrackingId
    }

    // Track the previous state to detect the exact true -> false transition
    var previousPrivateMode by remember { mutableStateOf(isPrivateMode) }

    LaunchedEffect(isPrivateMode) {
        if (previousPrivateMode && !isPrivateMode) {
            // Transitioned from Private -> Public
            // 1. Clear the current unsaved private chat from the screen
            // 2. Bump the refresh trigger to fetch the latest public chats
            uiState = uiState.copy(
                currentSession = null,
                messages = emptyList(),
                chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
            )
            // 3. Reset the tracking ID just to be safe
            privateSessionTrackingId = UUID.randomUUID().toString()
        }

        // Update the tracker for the next recomposition
        previousPrivateMode = isPrivateMode
    }

    // Map to hold background jobs tied to specific session IDs
    val activeGenerationJobs = remember { mutableStateMapOf<String, Job>() }

    // Map to hold the active text streams (Session ID -> Text)
    val activeGenerations = remember { mutableStateMapOf<String, String>() }

    // DERIVED STATE: This instantly appends the streaming text to the UI
    val displayMessages by remember(uiState.messages, uiState.currentSession, privateSessionTrackingId) {
        derivedStateOf {
            val currentId = getSessionId(uiState.currentSession)
            val generatingText = currentId?.let { activeGenerations[it] }

            if (generatingText != null) {
                uiState.messages + ChatMessage("assistant", generatingText, -1, false)
            } else {
                uiState.messages
            }
        }
    }

    val currentSessionId = getSessionId(uiState.currentSession)
    val isCurrentSessionGenerating = currentSessionId != null && activeGenerationJobs.containsKey(currentSessionId)

    // True if the model job is running, but hasn't sent any tokens back yet
    val isCurrentSessionThinking = remember(uiState.currentSession, privateSessionTrackingId, activeGenerations.size) {
        derivedStateOf {
            val currentId = getSessionId(uiState.currentSession)
            val hasJob = currentId?.let { activeGenerationJobs.containsKey(it) } == true
            val hasText = currentId?.let { activeGenerations.containsKey(it) } == true
            hasJob && !hasText
        }
    }.value

    val chatListState = rememberLazyListState()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val scrollKey = getSessionId(uiState.currentSession) ?: "empty"
    val chatScrollState = key(scrollKey) { rememberScrollState() }
    val messagePositions = key(scrollKey) { mutableStateMapOf<Int, Float>() }

    val installedOllamaModels = viewModel.installedOllamaModels
    val installedOllamaNames = viewModel.installedOllamaNames
    val installedComfyModelNames = viewModel.installedComfyModelNames

    val scrollToMessage: (ChatMessage) -> Unit = { message ->
        coroutineScope.launch {
            val index = uiState.messages.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                messagePositions[index]?.let { chatScrollState.animateScrollTo(it.toInt()) }
            }
        }
    }

    val onNextSearchResult: () -> Unit = {
        if (uiState.searchResults.isNotEmpty()) {
            val newIndex = (uiState.currentSearchIndex + 1) % uiState.searchResults.size
            uiState = uiState.copy(currentSearchIndex = newIndex)
            scrollToMessage(uiState.searchResults[newIndex])
        }
    }

    val onPrevSearchResult: () -> Unit = {
        if (uiState.searchResults.isNotEmpty()) {
            val newIndex =
                if (uiState.currentSearchIndex - 1 < 0) uiState.searchResults.size - 1 else uiState.currentSearchIndex - 1
            uiState = uiState.copy(currentSearchIndex = newIndex)
            scrollToMessage(uiState.searchResults[newIndex])
        }
    }

    var currentMode by remember { mutableStateOf(PromptMode.TEXT) }
    var selectedImageModel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(installedComfyModelNames) {
        when {
            installedComfyModelNames.isEmpty() -> selectedImageModel = null
            selectedImageModel.isNullOrEmpty() || selectedImageModel !in installedComfyModelNames -> selectedImageModel =
                installedComfyModelNames.first()
        }
    }

    fun triggerSearch(newText: String) {
        uiState = uiState.copy(searchText = newText)
        searchJob?.cancel()
        searchJob = coroutineScope.launch(Dispatchers.IO) {
            delay(250)
            val session = uiState.currentSession
            val pattern = uiState.searchText
            if (session == null || pattern.isBlank()) {
                withContext(Dispatchers.Main) { uiState = uiState.copy(searchResults = emptyList()) }
                return@launch
            }
            val matches = aiService.searchInChatSession(session, pattern, uiState.isCaseSensitive)
            withContext(Dispatchers.Main) {
                uiState = uiState.copy(searchResults = matches)
                if (matches.isNotEmpty()) {
                    val index = uiState.messages.indexOfFirst { it.id == matches.first().id }
                    if (index >= 0) {
                        coroutineScope.launch {
                            delay(50)
                            messagePositions[index]?.let { chatScrollState.animateScrollTo(it.toInt()) }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState.currentSession, uiState.isCaseSensitive) {
        if (uiState.searchText.isNotBlank()) triggerSearch(uiState.searchText)
        else uiState = uiState.copy(searchResults = emptyList())
    }

    // =========================================================
    // Image generation
    // =========================================================
    val generateImage: (String, String, Int, Int) -> Unit = let@{ pos, neg, w, h ->
        val comfyModelName = selectedImageModel ?: return@let
        val trimmedPrompt = pos.trim()
        if (trimmedPrompt.isEmpty()) return@let

        val currentSessionIdStr = getSessionId(uiState.currentSession)
        if (currentSessionIdStr != null && activeGenerationJobs.containsKey(currentSessionIdStr)) return@let

        val modelDto = downloadService.availableModels.find {
            it.model == comfyModelName || it.fullName == comfyModelName
        }

        if (modelDto == null) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Model configuration not found: $comfyModelName")
            }
            return@let
        }

        val sessionModelName = uiState.selectedModel ?: uiState.modelOptions.firstOrNull()?.name
        if (sessionModelName.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No text model selected for the session.")
            }
            return@let
        }

        uiState = uiState.copy(
            isGenerating = true, promptText = TextFieldValue("")
        )

        val userMessage = ChatMessage("user", trimmedPrompt, uiState.messages.size, false)

        coroutineScope.launch(Dispatchers.Main) {
            try {
                val sessionResult = withContext(Dispatchers.IO) {
                    var session = uiState.currentSession ?: ChatSession()
                    aiService.updateChatSession(session, userMessage, sessionModelName, isPrivateMode)
                }

                val sessionIdStr = sessionResult.id?.toString() ?: privateSessionTrackingId

                if (uiState.currentSession == null || getSessionId(uiState.currentSession) == sessionIdStr) {
                    uiState = uiState.copy(
                        currentSession = sessionResult,
                        messages = sessionResult.messages.toList(),
                        chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                    )
                } else {
                    uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                }

                val genJob = coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // Up to 3 total attempts (1 initial + 2 retries), each failure logged with
                        // full context and stack trace so ComfyUI/WSL2 issues are actually diagnosable.
                        val imageFile = retrying(
                            times = 3,
                            onAttemptFailed = { attempt, maxAttempts, error ->
                                log.error(
                                    "textToImage attempt $attempt/$maxAttempts failed for session " +
                                            "$sessionIdStr (model=$comfyModelName, prompt=\"$trimmedPrompt\")",
                                    error
                                )
                            }
                        ) { attempt ->
                            log.info(
                                "Requesting image generation (attempt $attempt/3) for session " +
                                        "$sessionIdStr, model=$comfyModelName, size=${w}x${h}"
                            )
                            visionService.textToImage(pos, neg, w, h, modelDto)
                                ?: throw IllegalStateException(
                                    "Vision service returned no image file (attempt $attempt)."
                                )
                        }

                        val assistantMessage =
                            ChatMessage("assistant", "<IMG>image<IMG>:${imageFile}", sessionResult.messages.size, false)
                        val finalSession = aiService.updateChatSession(
                            sessionResult, assistantMessage, sessionModelName, isPrivateMode
                        )

                        withContext(Dispatchers.Main) {
                            if (getSessionId(uiState.currentSession) == sessionIdStr) {
                                uiState = uiState.copy(
                                    currentSession = finalSession,
                                    messages = finalSession.messages.toList(),
                                    chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                                )
                            } else {
                                uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                            }
                        }
                    } catch (c: CancellationException) {
                        // Job was cancelled (e.g. user hit Cancel) - this is not a failure,
                        // so don't log it as an error or write an [Error] message to the chat.
                        log.info("Image generation cancelled for session $sessionIdStr")
                        throw c
                    } catch (t: Throwable) {
                        log.error(
                            "Image generation failed for session $sessionIdStr after retries " +
                                    "(model=$comfyModelName, prompt=\"$trimmedPrompt\")",
                            t
                        )
                        val errorMessage = ChatMessage(
                            "assistant",
                            "[Error] ${t.message ?: "Failed to generate image"}",
                            sessionResult.messages.size,
                            false
                        )
                        val finalSession =
                            aiService.updateChatSession(sessionResult, errorMessage, sessionModelName, isPrivateMode)

                        withContext(Dispatchers.Main) {
                            if (getSessionId(uiState.currentSession) == sessionIdStr) {
                                uiState = uiState.copy(
                                    currentSession = finalSession,
                                    messages = finalSession.messages.toList(),
                                    chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                                )
                            } else {
                                uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                            }
                        }
                    } finally {
                        withContext(NonCancellable + Dispatchers.Main) {
                            activeGenerationJobs.remove(sessionIdStr)
                            uiState = uiState.copy(isGenerating = false)
                        }
                    }
                }

                activeGenerationJobs[sessionIdStr] = genJob

            } catch (e: Exception) {
                log.error("Failed to create/update chat session before image generation", e)
                uiState = uiState.copy(isGenerating = false)
                snackbarHostState.showSnackbar("Failed to create chat session: ${e.message}")
            }
        }
    }

    fun updatePromptText(message: TextFieldValue) {
        uiState = uiState.copy(promptText = message)
    }

    val togglePin: (Long) -> Unit = { messageId ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val updatedMessage = aiService.toggleMessagePin(messageId)
                withContext(Dispatchers.Main) {
                    val index = uiState.messages.indexOfFirst { it.id == updatedMessage.id }
                    if (index != -1) {
                        val newMessages = uiState.messages.toMutableList()
                        newMessages[index] = updatedMessage
                        uiState =
                            uiState.copy(messages = newMessages, chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar(
                        message = e.message ?: "Failed to toggle pin", duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    LaunchedEffect("init") {
        withContext(Dispatchers.IO) {
            val distro = agentService.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO).getValueReference()
            val user = agentService.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME).getValueReference()
            val pass = agentService.getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD).getValueReference()

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(initStatusText = "Verifying system services (e.g. WSL)...")
            }

            val success = OSUtil.ensureWslStarted(distro, user, pass)
            if (!success) {
                withContext(Dispatchers.Main) {
                    uiState =
                        uiState.copy(initStatusText = "Failed to initiate system services. Please check your system.")
                }
                return@withContext
            }

            withContext(Dispatchers.Main) {
                uiState = uiState.copy(initStatusText = "Loading installed models...")
            }
        }

        downloadService.refreshSync()
        uiState = uiState.copy(isInitializing = false)
        viewModel.checkForUpdates()
    }

    LaunchedEffect(installedOllamaNames) {
        val current = uiState.selectedModel
        val fixed = when {
            installedOllamaNames.isEmpty() -> null
            current != null && current in installedOllamaNames -> current
            else -> installedOllamaNames.first()
        }
        if (fixed != uiState.selectedModel) {
            uiState = uiState.copy(selectedModel = fixed)
        }
    }

    val submitPrompt: () -> Unit = let@{
        val trimmedPrompt = uiState.promptText.text.trim()
        if (trimmedPrompt.isEmpty()) return@let

        val currentSessionIdStr = getSessionId(uiState.currentSession)
        if (currentSessionIdStr != null && activeGenerationJobs.containsKey(currentSessionIdStr)) return@let

        val modelName = uiState.selectedModel ?: uiState.modelOptions.firstOrNull()?.name
        if (modelName.isNullOrEmpty()) return@let

        val userMessage = ChatMessage("user", trimmedPrompt, uiState.messages.size, false)

        uiState = uiState.copy(promptText = TextFieldValue(""))

        coroutineScope.launch(Dispatchers.IO) {
            var session = uiState.currentSession ?: ChatSession()
            session = aiService.updateChatSession(session, userMessage, modelName, isPrivateMode)

            val sessionIdStr = session.id?.toString() ?: privateSessionTrackingId

            withContext(Dispatchers.Main) {
                if (uiState.currentSession == null || getSessionId(uiState.currentSession) == sessionIdStr) {
                    uiState = uiState.copy(
                        currentSession = session,
                        messages = session.messages,
                        chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                    )
                } else {
                    uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                }
            }

            val streamJob = launch {
                try {
                    val historySnapshot = session.messages.toList()
                    val fullResponse = StringBuilder()

                    streamingService.streamChat(modelName, historySnapshot).collect { chunk ->
                        fullResponse.append(chunk)
                        withContext(Dispatchers.Main) {
                            activeGenerations[sessionIdStr] = fullResponse.toString()
                        }
                    }

                    val finalAssistantMessage =
                        ChatMessage("assistant", fullResponse.toString(), session.messages.size, false)
                    val finalSession =
                        aiService.updateChatSession(session, finalAssistantMessage, modelName, isPrivateMode)

                    withContext(Dispatchers.Main) {
                        activeGenerations.remove(sessionIdStr)
                        if (getSessionId(uiState.currentSession) == sessionIdStr) {
                            uiState = uiState.copy(
                                currentSession = finalSession,
                                messages = finalSession.messages,
                                chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                            )
                        } else {
                            uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                        }
                    }

                } catch (t: Throwable) {
                    val errorMessage = ChatMessage(
                        "assistant",
                        "[Error] ${t.message ?: "Failed to generate response"}",
                        session.messages.size,
                        false
                    )
                    val finalSession = aiService.updateChatSession(session, errorMessage, modelName, isPrivateMode)

                    withContext(Dispatchers.Main) {
                        activeGenerations.remove(sessionIdStr)
                        if (getSessionId(uiState.currentSession) == sessionIdStr) {
                            uiState = uiState.copy(
                                currentSession = finalSession,
                                messages = finalSession.messages,
                                chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                            )
                        }
                    }
                } finally {
                    activeGenerationJobs.remove(sessionIdStr)
                }
            }

            activeGenerationJobs[sessionIdStr] = streamJob
        }
    }

    val cancelGeneration: () -> Unit = {
        val sessionIdStr = getSessionId(uiState.currentSession)

        if (sessionIdStr != null) {
            activeGenerationJobs[sessionIdStr]?.cancel()
            activeGenerationJobs.remove(sessionIdStr)
            activeGenerations.remove(sessionIdStr)
        }

        uiState = uiState.copy(isGenerating = false, promptText = TextFieldValue(""))
    }

    Surface() {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Sidebar(
                    aiService = aiService,
                    viewModel = viewModel,
                    activeSessionId = uiState.currentSession?.id, // Fine keeping this since private chats don't appear in sidebar
                    refreshTrigger = uiState.chatsRefreshTrigger,
                    isCollapsed = isSidebarCollapsed,
                    onToggleSidebar = onToggleSidebar,
                    onSettings = { onSettings("general") },
                    onSessionSelected = { session ->
                        val restoredModel = session.promptModel?.takeIf { it.isNotBlank() } ?: uiState.selectedModel

                        uiState = uiState.copy(
                            currentSession = session,
                            messages = session.messages,
                            selectedModel = restoredModel,
                            promptText = TextFieldValue(""),
                            isGenerating = false
                        )
                    },
                    onNewChat = {
                        uiState = uiState.copy(currentSession = null, messages = emptyList())
                        // REGENERATE FALLBACK ID to cleanly separate the old transient session from the new one
                        privateSessionTrackingId = UUID.randomUUID().toString()
                    },
                    onProvider = onProvider
                )

                Box(modifier = Modifier.weight(1f)) {
                    MainHubContent(
                        isSidebarCollapsed = uiState.isSidebarCollapsed,
                        onToggleSidebar = { uiState = uiState.copy(isSidebarCollapsed = !uiState.isSidebarCollapsed) },
                        userMenuOpen = uiState.userMenuOpen,
                        onUserMenuOpenChange = { uiState = uiState.copy(userMenuOpen = it) },
                        modelMenuOpen = uiState.modelMenuOpen,
                        onModelMenuOpenChange = { uiState = uiState.copy(modelMenuOpen = it) },
                        modelOptions = if (currentMode == PromptMode.TEXT) {
                            installedOllamaModels
                        } else {
                            installedComfyModelNames.map {
                                PromptModel(it, "", "", "COMFY_UI")
                            }
                        },
                        selectedModel = if (currentMode == PromptMode.TEXT) {
                            uiState.selectedModel
                        } else {
                            selectedImageModel
                        },
                        onSelectModel = { selectedName ->
                            if (currentMode == PromptMode.TEXT) {
                                uiState = uiState.copy(
                                    selectedModel = selectedName, currentSession = null, messages = emptyList()
                                )
                            } else {
                                selectedImageModel = selectedName
                            }
                        },
                        hasActiveSession = uiState.messages.isNotEmpty(),
                        promptText = uiState.promptText,
                        onPromptChange = ::updatePromptText,
                        onSendPrompt = submitPrompt,
                        chatMessages = displayMessages, // Now correctly bound to the derived streaming list!
                        pinnedMessages = uiState.pinnedMessages,
                        onTogglePin = togglePin,
                        chatScrollState = chatScrollState,
                        messagePositions = messagePositions,
                        isGeneratingResponse = isCurrentSessionGenerating,
                        onCancel = cancelGeneration,
                        onGenerateImage = generateImage,
                        onToggleRightSidebar = {
                            uiState = uiState.copy(isRightSidebarOpen = !uiState.isRightSidebarOpen)
                        },
                        searchText = uiState.searchText,
                        onSearchTextChange = ::triggerSearch,
                        isCaseSensitive = uiState.isCaseSensitive,
                        installedComfyModels = installedComfyModelNames,
                        onCaseSensitiveChange = { uiState = uiState.copy(isCaseSensitive = it) },
                        matchCount = uiState.searchResults.size,
                        currentMatchIndex = uiState.currentSearchIndex,
                        onNextMatch = onNextSearchResult,
                        onPrevMatch = onPrevSearchResult,
                        activeMessageId = uiState.activeMessageId,
                        isPrivateMode = isPrivateMode,
                        isRightSidebarOpen = uiState.isRightSidebarOpen,
                        isThinking = isCurrentSessionThinking,
                        onSettings = onSettings,
                        onPrivateModeChange = onPrivateModeChange,
                        currentMode = currentMode,
                        onModeChange = { currentMode = it },
                        sessionId = getSessionId(uiState.currentSession)
                    )
                    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
                }

                RightSidebar(
                    isOpen = uiState.isRightSidebarOpen,
                    onClose = { uiState = uiState.copy(isRightSidebarOpen = false) },
                    pinnedMessages = uiState.pinnedMessages,
                    onUnpin = togglePin,
                    onMessageClick = scrollToMessage
                )
            }
        }

        if (uiState.isInitializing) {
            AppPortal {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }, contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(RadiusMedium),
                        color = NextGpuTheme.colors.surface,
                        modifier = Modifier.widthIn(min = 400.dp).padding(SpacingLarge)
                    ) {
                        Column(
                            modifier = Modifier.padding(SpacingExtraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(30.dp),
                                color = NextGpuTheme.colors.primaryVariant,
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = uiState.initStatusText,
                                style = MaterialTheme.typography.body1,
                                color = NextGpuTheme.colors.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}