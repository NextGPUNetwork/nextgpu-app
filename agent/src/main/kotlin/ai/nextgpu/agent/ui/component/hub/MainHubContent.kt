package ai.nextgpu.agent.ui.component.hub

import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.model.PromptModel
import ai.nextgpu.agent.ui.theme.MaxContentWidth
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusExtraLarge
import ai.nextgpu.agent.ui.theme.RadiusLarge
import ai.nextgpu.agent.ui.theme.RadiusMedium
import ai.nextgpu.agent.ui.theme.SpacingLarge
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun MainHubContent(
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    userMenuOpen: Boolean,
    onUserMenuOpenChange: (Boolean) -> Unit,
    modelMenuOpen: Boolean,
    onModelMenuOpenChange: (Boolean) -> Unit,
    modelOptions: List<PromptModel>,
    selectedModel: String?,
    onSelectModel: (String) -> Unit,
    hasActiveSession: Boolean,
    promptText: TextFieldValue,
    onPromptChange: (TextFieldValue) -> Unit,
    onSendPrompt: () -> Unit,
    chatMessages: List<ChatMessage>,
    pinnedMessages: List<ChatMessage>,
    onTogglePin: (Long) -> Unit,
    onToggleRightSidebar: () -> Unit,
    chatScrollState: ScrollState,
    messagePositions: MutableMap<Int, Float>,
    isGeneratingResponse: Boolean,
    onCancel: () -> Unit,
    installedComfyModels: List<String>,
    onGenerateImage: (String, String, Int, Int) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isCaseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    activeMessageId: Long?,
    isPrivateMode: Boolean,
    onPrivateModeChange: (Boolean) -> Unit,
    isRightSidebarOpen: Boolean,
    isThinking: Boolean,
    onSettings: (tabId: String) -> Unit,
    currentMode: PromptMode,
    onModeChange: (PromptMode) -> Unit,
    sessionId: String?,
) {
    val aiService = remember { Services.aiService }
    val audioRecorderService = remember { Services.audioRecorderService }
    val nextGpuWebService = remember { Services.nextGpuWebService }

    Column(modifier = Modifier.fillMaxHeight()
        .fillMaxWidth()
        .clip(RoundedCornerShape(topStart = RadiusMedium))
        .background(NextGpuTheme.colors.background)) {
        TopNavigation(
            isSidebarCollapsed = isSidebarCollapsed,
            onToggleSidebar = onToggleSidebar,
            userMenuOpen = userMenuOpen,
            onUserMenuOpenChange = onUserMenuOpenChange,
            modelMenuOpen = modelMenuOpen,
            onModelMenuOpenChange = onModelMenuOpenChange,
            modelOptions = modelOptions,
            selectedModel = selectedModel,
            onSelectModel = onSelectModel,
            hasActiveSession = hasActiveSession,
            searchText = searchText,
            onSearchTextChange = onSearchTextChange,
            isCaseSensitive = isCaseSensitive,
            onCaseSensitiveChange = onCaseSensitiveChange,
            matchCount = matchCount,
            currentMatchIndex = currentMatchIndex,
            onNextMatch = onNextMatch,
            onPrevMatch = onPrevMatch,
            isPrivateMode = isPrivateMode,
            onPrivateModeChange = onPrivateModeChange,
            hasPinnedMessages = pinnedMessages.isNotEmpty(),
            onToggleRightSidebar = onToggleRightSidebar,
            isRightSidebarOpen = isRightSidebarOpen,
            currentMode = currentMode,
        )

        // Chat area (centered content)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = SpacingLarge),
            contentAlignment = Alignment.Center
        ) {
            if (chatMessages.isEmpty()) {
                Column(
                    modifier = Modifier.widthIn(max = MaxContentWidth) // THEME: Restrict width for empty state
                        .fillMaxWidth(), horizontalAlignment = Alignment.Start
                ) {
                    Greeting()
                    Spacer(modifier = Modifier.height(SpacingLarge))
//                    SuggestionCards(aiService = aiService, onPromptSelected = { onPromptChange(TextFieldValue(it)) })
                }
            } else {
                ChatSection(
                    messages = chatMessages,
                    onPromptChange = onPromptChange,
                    onTogglePin = onTogglePin,
                    isGenerating = isGeneratingResponse,
                    scrollState = chatScrollState, // CHANGED
                    messagePositions = messagePositions, // ADDED
                    searchText = searchText,
                    isCaseSensitive = isCaseSensitive,
                    activeMessageId = activeMessageId,
                    isThinking = isThinking,
                )
            }
        }

        PromptRegion(
            sessionId = sessionId,
            promptText = promptText,
            onPromptChange = onPromptChange,
            onSendPrompt = onSendPrompt,
            isGenerating = isGeneratingResponse,
            onCancel = onCancel,
            installedComfyModels = installedComfyModels,
            onGenerateImage = onGenerateImage,
            onOpenModelSettings = { onSettings("models") },
            currentMode = currentMode,
            onModeChange = onModeChange,
            audioRecorderService = audioRecorderService,
            onCheckSttHealth = {
                nextGpuWebService.isSttServiceAvailable()
            },
            onTranscribeAudio = { file ->
                val result = nextGpuWebService.getTransformedString(file)
                result.text
            }
        )
    }
}