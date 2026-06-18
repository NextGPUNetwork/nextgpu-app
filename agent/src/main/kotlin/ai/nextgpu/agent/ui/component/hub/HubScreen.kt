package ai.nextgpu.agent.ui.component.hub

import ai.nextgpu.agent.OllamaStreamingService
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.model.ChatSession
import ai.nextgpu.agent.model.PromptModel
import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.NextGpuAiService
import ai.nextgpu.agent.service.NextGpuVisionService
import ai.nextgpu.agent.springContext
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


object Services {
    val agentService: NextGpuAgentService get() = springContext.getBean(NextGpuAgentService::class.java)
    val aiService: NextGpuAiService get() = springContext.getBean(NextGpuAiService::class.java)
    val visionService: NextGpuVisionService get() = springContext.getBean(NextGpuVisionService::class.java)
    val streamingService: OllamaStreamingService get() = springContext.getBean(OllamaStreamingService::class.java)
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
) {
    val agentService = remember { Services.agentService }
    val aiService = remember { Services.aiService }
    val visionService = remember { Services.visionService }
    val streamingService = remember { Services.streamingService }
    val downloadService = remember { springContext.getBean(ModelDownloadService::class.java) }

    var uiState by remember { mutableStateOf(HubUiState()) }
    val coroutineScope = rememberCoroutineScope()

    val snackbarHostState = remember { SnackbarHostState() }
    // Map to hold background jobs tied to specific session IDs
    val activeGenerationJobs = remember { mutableStateMapOf<String, Job>() }

    // Map to hold the active text streams (Session ID -> Text)
    val activeGenerations = remember { mutableStateMapOf<String, String>() }

    // DERIVED STATE: This instantly appends the streaming text to the UI
    // without copying/mutating the main list. It is incredibly fast.
    val displayMessages by remember(uiState.messages, uiState.currentSession?.id) {
        derivedStateOf {
            val currentId = uiState.currentSession?.id?.toString()
            val generatingText = currentId?.let { activeGenerations[it] }

            if (generatingText != null) {
                // Append a local, temporary message using -1 as a fake ID
                uiState.messages + ChatMessage("assistant", generatingText, -1, false)
            } else {
                uiState.messages
            }
        }
    }
// In HubScreen, replace isCurrentSessionGenerating with this:
    val currentSessionId = uiState.currentSession?.id?.toString()

// This accurately reflects "Is the chat I'm currently looking at busy?"
    val isCurrentSessionGenerating = currentSessionId != null && activeGenerationJobs.containsKey(currentSessionId)

    // True if the model job is running, but hasn't sent any tokens back yet
    val isCurrentSessionThinking = remember(uiState.currentSession?.id, activeGenerations.size) {
        derivedStateOf {
            val currentId = uiState.currentSession?.id?.toString()
            val hasJob = currentId?.let { activeGenerationJobs.containsKey(it) } == true
            val hasText = currentId?.let { activeGenerations.containsKey(it) } == true
            hasJob && !hasText // Job exists but no stream text has arrived yet
        }
    }.value

    val chatListState = rememberLazyListState()
    var searchJob by remember { mutableStateOf<Job?>(null) }

    val chatScrollState = key(uiState.currentSession?.id) { rememberScrollState() }
    val messagePositions = key(uiState.currentSession?.id) { mutableStateMapOf<Int, Float>() }

    // =========================================================
    // Installed-model lists, derived straight from the service's
    // snapshot state. No StateFlow, no counter, no re-fetch.
    //
    // downloadService.installedModels is a mutableStateOf in the service.
    // Reading it inside derivedStateOf creates a real Compose dependency,
    // so the moment refreshSync() rewrites it (after any download/delete/
    // manual refresh), these recompute and every reader recomposes - the
    // exact same mechanism that already updates the download-progress UI
    // inside the Settings popup.
    // =========================================================

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

    // Move auto-select logic here (previously in PromptRegion)
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

        // Prevent overlapping generation requests in the same session
        val currentSessionIdStr = uiState.currentSession?.id?.toString()
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

        // FIX 1: Grab the TEXT model to handle the session & title generation
        val sessionModelName = uiState.selectedModel ?: uiState.modelOptions.firstOrNull()?.name
        if (sessionModelName.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No text model selected for the session.")
            }
            return@let
        }

        // Instantly lock UI, clear text box, and show generating state
        uiState = uiState.copy(
            isGenerating = true, promptText = TextFieldValue("")
        )

        val userMessage = ChatMessage("user", trimmedPrompt, uiState.messages.size, false)

        // Launch on Main to ensure atomic UI state updates before shifting to IO
        coroutineScope.launch(Dispatchers.Main) {
            try {
                // Save session on IO using the TEXT model (prevents title-generation crash)
                val sessionResult = withContext(Dispatchers.IO) {
                    var session = uiState.currentSession ?: ChatSession()
                    aiService.updateChatSession(session, userMessage, sessionModelName, isPrivateMode)
                }
                val sessionIdStr = sessionResult.id.toString()

                // Update UI to show the user's prompt immediately
                if (uiState.currentSession == null || uiState.currentSession?.id?.toString() == sessionIdStr) {
                    uiState = uiState.copy(
                        currentSession = sessionResult,
                        messages = sessionResult.messages.toList(),
                        chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                    )
                } else {
                    uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                }

                // Launch Image Generation Job
                val genJob = coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // Pass the ComfyUI model exclusively to the Vision Service
                        val imageFile = visionService.textToImage(pos, neg, w, h, modelDto)
                        if (imageFile == null) throw IllegalStateException("Vision service failed to return an image file.")

                        val assistantMessage =
                            ChatMessage("assistant", "<IMG>image<IMG>:${imageFile}", sessionResult.messages.size, false)
                        val finalSession = aiService.updateChatSession(
                            sessionResult, assistantMessage, sessionModelName, isPrivateMode
                        )

                        withContext(Dispatchers.Main) {
                            if (uiState.currentSession?.id?.toString() == sessionIdStr) {
                                uiState = uiState.copy(
                                    currentSession = finalSession,
                                    messages = finalSession.messages.toList(),
                                    chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1
                                )
                            } else {
                                uiState = uiState.copy(chatsRefreshTrigger = uiState.chatsRefreshTrigger + 1)
                            }
                        }
                    } catch (t: Throwable) {
                        val errorMessage = ChatMessage(
                            "assistant",
                            "[Error] ${t.message ?: "Failed to generate image"}",
                            sessionResult.messages.size,
                            false
                        )
                        val finalSession =
                            aiService.updateChatSession(sessionResult, errorMessage, sessionModelName, isPrivateMode)

                        withContext(Dispatchers.Main) {
                            if (uiState.currentSession?.id?.toString() == sessionIdStr) {
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
                        withContext(Dispatchers.Main) {
                            activeGenerationJobs.remove(sessionIdStr)
                            uiState = uiState.copy(isGenerating = false)
                        }
                    }
                }

                // Track job synchronously on Main.
                activeGenerationJobs[sessionIdStr] = genJob

            } catch (e: Exception) {
                // FIX 2: Safety net. If session creation crashes, unlock the UI instantly.
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

    // =========================================================
    // WSL setup + initial model load.
    //
    // This effect now OWNS initialization: it awaits the first
    // refreshSync() (dispatcher-safe, fetches on IO) and then clears the
    // loading overlay. Ongoing updates after this are handled purely by
    // the derivedStateOf reads above - no counter needed.
    // =========================================================
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

        // Await the first load, then drop the overlay. refreshSync() writes
        // installedModels, so the derived lists below are populated by the
        // time we clear isInitializing.
        downloadService.refreshSync()
        uiState = uiState.copy(isInitializing = false)

        // Version check after WSL is checked and models are loaded
        viewModel.checkForUpdates()
    }

    // =========================================================
    // Selection reconciliation - the ONLY stateful piece left.
    //
    // Keyed on the installed Ollama NAMES (Strings -> reliable structural
    // equality), so it runs only when the installed set actually changes.
    // It keeps the current selection if still valid, falls back to the
    // first model otherwise, or null if none remain.
    // =========================================================
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

        val currentSessionIdStr = uiState.currentSession?.id?.toString()
        if (currentSessionIdStr != null && activeGenerationJobs.containsKey(currentSessionIdStr)) return@let

        val modelName = uiState.selectedModel ?: uiState.modelOptions.firstOrNull()?.name
        if (modelName.isNullOrEmpty()) return@let

        val userMessage = ChatMessage("user", trimmedPrompt, uiState.messages.size, false)

        uiState = uiState.copy(promptText = TextFieldValue(""))

        coroutineScope.launch(Dispatchers.IO) {
            var session = uiState.currentSession ?: ChatSession()
            session = aiService.updateChatSession(session, userMessage, modelName, isPrivateMode)
            val sessionIdStr = session.id.toString()

            withContext(Dispatchers.Main) {
                // CRITICAL: We DO NOT put "..." here anymore.
                // The isCurrentSessionThinking state handles the visual loading placeholder.
                if (uiState.currentSession == null || uiState.currentSession?.id?.toString() == sessionIdStr) {
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
                            // The very first token replaces the thinking state seamlessly
                            activeGenerations[sessionIdStr] = fullResponse.toString()
                        }
                    }

                    val finalAssistantMessage =
                        ChatMessage("assistant", fullResponse.toString(), session.messages.size, false)
                    val finalSession =
                        aiService.updateChatSession(session, finalAssistantMessage, modelName, isPrivateMode)

                    withContext(Dispatchers.Main) {
                        activeGenerations.remove(sessionIdStr)
                        if (uiState.currentSession?.id?.toString() == sessionIdStr) {
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
                        if (uiState.currentSession?.id?.toString() == sessionIdStr) {
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
        val sessionIdStr = uiState.currentSession?.id?.toString()

        if (sessionIdStr != null) {
            activeGenerationJobs[sessionIdStr]?.cancel()
            activeGenerationJobs.remove(sessionIdStr)
            activeGenerations.remove(sessionIdStr) // <-- Clears the overlay
        }

        uiState = uiState.copy(isGenerating = false, promptText = TextFieldValue(""))
    }

    Surface(color = NextGpuTheme.colors.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Sidebar(
                    aiService = aiService,
                    viewModel = viewModel,
                    activeSessionId = uiState.currentSession?.id,
                    refreshTrigger = uiState.chatsRefreshTrigger,
                    isCollapsed = uiState.isSidebarCollapsed,
                    onToggleSidebar = { uiState = uiState.copy(isSidebarCollapsed = !uiState.isSidebarCollapsed) },
                    onSettings = { onSettings("general") },
                    onSessionSelected = { session ->
                        val restoredModel = session.promptModel?.takeIf { it.isNotBlank() } ?: uiState.selectedModel

                        // FIX: Reset prompt text and generating state when switching sessions
                        // so we don't carry over stale "Generating..." text or input blocks.
                        uiState = uiState.copy(
                            currentSession = session,
                            messages = session.messages,
                            selectedModel = restoredModel,
                            promptText = TextFieldValue(""), // Clear input
                            isGenerating = false             // Force unlock input
                        )
                    },
                    onNewChat = { uiState = uiState.copy(currentSession = null, messages = emptyList()) })

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
                        chatMessages = uiState.messages,
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
            Footer(viewModel = viewModel)
        }

        if (uiState.isInitializing) {
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
                        CircularProgressIndicator(color = NextGpuTheme.colors.primaryVariant, strokeWidth = 3.dp)
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

