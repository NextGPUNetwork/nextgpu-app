package ai.nextgpu.agent.ui.component.popup.settings.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.VersionUpdateService
import ai.nextgpu.agent.ui.theme.AppThemeMode
import ai.nextgpu.agent.util.OSUtil
import ai.nextgpu.common.dto.AiModelDto
import ai.nextgpu.common.model.AiModelRegistry
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val agentService: NextGpuAgentService,
    private val downloadService: ModelDownloadService,
    private val versionUpdateService: VersionUpdateService
) {
    private val viewModelScope = CoroutineScope(Dispatchers.IO)

    // --- Core Settings State ---
    var appThemeMode by mutableStateOf(AppThemeMode.System)
        private set
    var isPrivateMode by mutableStateOf(false)
        private set
    var isAdvancedMode by mutableStateOf(false)
        private set

    // NEW: OpenClaw Setup State
    var isOpenclawSetupComplete by mutableStateOf(false)
        private set

    // --- Update Service State ---
    val isUpdateAvailable get() = versionUpdateService.isUpdateAvailable
    val latestVersionInfo get() = versionUpdateService.latestVersionInfo
    val isUpdateDownloading get() = versionUpdateService.isDownloading
    val updateDownloadProgress get() = versionUpdateService.downloadProgress
    val updateError get() = versionUpdateService.updateError

    // --- Direct Exposure of Download Service State (1:1 naming) ---
    val isLoading get() = downloadService.isLoading
    val availableModels get() = downloadService.availableModels
    val installedModelNames get() = downloadService.installedModelNames
    val downloadingModels get() = downloadService.downloadingModels
    val pausedModels get() = downloadService.pausedModels
    val stoppingModels get() = downloadService.stoppingModels
    val deletingModels get() = downloadService.deletingModels
    val downloadingProgress get() = downloadService.downloadingProgress
    val ollamaDownloadingCount get() = downloadService.ollamaDownloadingCount

    // Inside SettingsViewModel
    val installedOllamaModels get() = downloadService.installedModels
        .filter { it.aiModelRegistry == AiModelRegistry.OLLAMA.name }

    val installedOllamaNames get() = installedOllamaModels.map { it.name }

    val installedComfyModelNames get() = downloadService.installedModels
        .filter { it.aiModelRegistry == AiModelRegistry.COMFY_UI.name }
        .map { it.name }

    var showUpdatePopup by mutableStateOf(versionUpdateService.isUpdateAvailable)
        private set

    val currentAppVersion get() = versionUpdateService.currentVersion

    var showOpenclawShortcut by mutableStateOf(false)
        private set

    init {
        loadInitialData()
        viewModelScope.launch {
            snapshotFlow { versionUpdateService.isUpdateAvailable }
                .collectLatest { isAvailable ->
                    // Auto-show the popup when the background check finds an update
                    if (isAvailable) {
                        showUpdatePopup = true
                    }
                }
        }
    }

    private fun loadInitialData() {
        val savedTheme = agentService.getGlobalProperty(GlobalPropertyConfig.APP_THEME)
            ?.getValue<String>() ?: "System"
        appThemeMode = AppThemeMode.valueOf(savedTheme)

        isPrivateMode = agentService.getGlobalProperty(GlobalPropertyConfig.IS_PRIVATE_MODE)
            ?.valueReference?.toBoolean() ?: false

        isAdvancedMode = agentService.getGlobalProperty(GlobalPropertyConfig.IS_ADVANCED_MODE)
            ?.valueReference?.toBoolean() ?: false

        isOpenclawSetupComplete = agentService.getGlobalProperty(GlobalPropertyConfig.IS_OPENCLAW_SETUP_COMPLETED)
            ?.valueReference?.toBoolean() ?: false

        showOpenclawShortcut = agentService.getGlobalProperty(GlobalPropertyConfig.SHOW_OPENCLAW_SHORTCUT)
            ?.valueReference?.toBoolean() ?: false

        viewModelScope.launch { downloadService.refresh() }
    }

    // --- Settings Methods ---
    fun updateTheme(newTheme: AppThemeMode) {
        appThemeMode = newTheme
        viewModelScope.launch { agentService.updateAppTheme(newTheme.name) }
    }

    fun updatePrivateMode(enabled: Boolean) {
        isPrivateMode = enabled
        viewModelScope.launch { agentService.togglePrivateMode(enabled) }
    }

    fun updateAdvancedMode(enabled: Boolean) {
        isAdvancedMode = enabled
        viewModelScope.launch { agentService.toggleAdvancedMode(enabled) }
    }

    // NEW: Update OpenClaw Setup Status
    fun updateOpenclawSetupComplete(completed: Boolean) {
        isOpenclawSetupComplete = completed
        viewModelScope.launch { agentService.updateIsOpenclawSetupCompleteProperty(completed) }
    }

    // Method to control the popup from anywhere (like your settings page)
    fun setUpdatePopupVisibility(visible: Boolean) {
        showUpdatePopup = visible
    }

    // --- On-Demand URL Fetching ---
    suspend fun fetchOpenclawDashboardUrl(): String {
        return withContext(Dispatchers.IO) {
            val distro = agentService.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO)?.valueReference ?: "nextgpu"
            val username = agentService.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME)?.valueReference ?: "nextgpu"

            val token = OSUtil.getOpenclawGatewayToken(distro, username)
            if (!token.isNullOrBlank()) {
                "http://127.0.0.1:18789/?token=$token"
            } else {
                "http://127.0.0.1:18789/" // Safe fallback
            }
        }
    }

    fun toggleOpenclawShortcut(enabled: Boolean) {
        showOpenclawShortcut = enabled
        viewModelScope.launch { agentService.toggleOpenclawShortcut(enabled) }
    }

    fun uninstallOpenclaw() {
        viewModelScope.launch {
            val success = OSUtil.uninstallOpenclaw() // Now runs silently and waits!
            if (success) {
                updateOpenclawSetupComplete(false)
            }
        }
    }

    // --- Model Management Methods ---
    fun launchDownload(model: AiModelDto) = downloadService.launchDownload(model)
    fun pauseDownload(key: String) = downloadService.pauseDownload(key)
    fun resumeDownload(model: AiModelDto) = downloadService.resumeDownload(model)
    fun stopDownload(model: AiModelDto) = downloadService.stopDownload(model)
    fun deleteModel(model: AiModelDto) = downloadService.deleteModel(model)

    fun checkForUpdates() = versionUpdateService.checkForUpdates()
    fun snoozeUpdate() {
        showUpdatePopup = false
    }
    fun startUpdate() = versionUpdateService.startDownloadAndInstall()
    fun cancelUpdate() = versionUpdateService.cancelDownload()
}