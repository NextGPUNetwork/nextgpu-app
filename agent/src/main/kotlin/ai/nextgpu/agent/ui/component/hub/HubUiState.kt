package ai.nextgpu.agent.ui.component.hub

import androidx.compose.ui.text.input.TextFieldValue
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.model.ChatSession
import ai.nextgpu.agent.model.PromptModel

data class HubUiState(
    val currentSession: ChatSession? = null,
    val messages: List<ChatMessage> = emptyList(),

    // --- Initialization State ---
    val isInitializing: Boolean = true,
    val initStatusText: String = "Starting NextGPU Engine...",

    // Prompt / generation
    val promptText: TextFieldValue = TextFieldValue(""),
    val isGenerating: Boolean = false,
    val selectedModel: String? = null,
    val modelOptions: List<PromptModel> = emptyList(),

    // UI chrome
    val isSidebarCollapsed: Boolean = false,
    val isRightSidebarOpen: Boolean = false,
    val userMenuOpen: Boolean = false,
    val modelMenuOpen: Boolean = false,

    // Search
    val searchText: String = "",
    val isCaseSensitive: Boolean = false,
    val searchResults: List<ChatMessage> = emptyList(),
    val currentSearchIndex: Int = 0,

    // Dialogs
    val showImageDialog: Boolean = false,

    // Installed ComfyUI model names (AiModelDto.model values).
    // Populated from aiService.listDownloadedModels() filtered by COMFY_UI registry.
    // Updated reactively via ModelDownloadService.onInstalledModelsChanged callback.
    val installedComfyModels: List<String> = emptyList(),

    // "imperative UI refresh" knobs
    val chatsRefreshTrigger: Int = 0,
) {
    val pinnedMessages: List<ChatMessage> get() = messages.filter { it.pinned == true }

    val activeMessageId: Long?
        get() = searchResults.getOrNull(currentSearchIndex)?.id
}