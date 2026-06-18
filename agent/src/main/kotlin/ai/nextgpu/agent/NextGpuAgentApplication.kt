package ai.nextgpu.agent

import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.AnalyticsService
import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.VersionUpdateService
import ai.nextgpu.agent.ui.*
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.hub.HubScreen
import ai.nextgpu.agent.ui.component.popup.settings.SettingsPopup
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.component.popup.settings.modelmanagment.SimpleProgressBar
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.util.OSUtil
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import org.slf4j.LoggerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.getBean
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPasswordField

@SpringBootApplication
@ComponentScan(
    basePackages = [
        "ai.nextgpu.agent",
        "ai.nextgpu.common",
    ]
)
@EntityScan(
    basePackages = ["ai.nextgpu.common.model", "ai.nextgpu.common.report", "ai.nextgpu.agent.model"]
)
open class NextGPUAgentApplication

private val logger = LoggerFactory.getLogger(NextGPUAgentApplication::class.java)

lateinit var springContext: ConfigurableApplicationContext


fun main() {
    System.setProperty("java.awt.headless", "false")

    springContext = SpringApplicationBuilder(NextGPUAgentApplication::class.java).headless(false)
        .web(WebApplicationType.NONE)   // Declare as a non-web server application
        .run()

    val analyticsService = springContext.getBean<AnalyticsService>()

    Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
        analyticsService.notifyCrashReport(throwable)
        logger.error("Uncaught exception", throwable)
    }

    GlobalPropertyConfig.APPLICATION_UP_TIMESTAMP = System.currentTimeMillis()
    springContext.getBean<GlobalPropertyConfig>().InitializeGlobalProperties()

    fun exitCleanly(exitApplication: () -> Unit) {
        // Notify the application uptime before the application exits
        runCatching {
            analyticsService.notifyApplicationUpTime()
        }

        runCatching {
            OSUtil.keepAliveProcess.destroy()
        }

        runCatching {
            springContext.close()
        }

        exitApplication()
    }
    application {

        // --- NEW WINDOW STATE LOGIC ---
        var isWindowResizable by remember { mutableStateOf(false) }
        var windowWidth by remember { mutableStateOf(1200.dp) }
        var windowHeight by remember { mutableStateOf(820.dp) }

        var showExitDialog by remember { mutableStateOf(false) }

        val windowState = rememberWindowState(
            width = windowWidth, height = windowHeight, position = WindowPosition(Alignment.Center)
        )

        // Force the OS window to physically update when our state variables change
        LaunchedEffect(windowWidth, windowHeight) {
            windowState.size = androidx.compose.ui.unit.DpSize(windowWidth, windowHeight)
            windowState.position = WindowPosition(Alignment.Center)
        }

        val modelDownloadService = remember { springContext.getBean<ModelDownloadService>() }
        val versionUpdateService = remember { springContext.getBean<VersionUpdateService>() }

        Window(
            onCloseRequest = {
                if (modelDownloadService.downloadingModels.isNotEmpty() || versionUpdateService.isDownloading) {
                    showExitDialog = true
                } else {
                    exitCleanly(::exitApplication)
                }
            },
            title = "NextGPU Hub - Private and secure AI compute",
            icon = painterResource("images/nextgpu-primary-logo.svg"),
            state = windowState,
            resizable = isWindowResizable,
        ) {
            App(
                onFatalError = { message ->
                analyticsService.notifyCrashReport(RuntimeException(message))
                JOptionPane.showMessageDialog(null, message, "NextGPU – Error", JOptionPane.ERROR_MESSAGE)
                exitCleanly(::exitApplication)
            },
                // Update our state variables from the App's callback
                onWindowModeChange = { resizable, targetWidth, targetHeight ->
                    isWindowResizable = resizable
                    windowWidth = targetWidth
                    windowHeight = targetHeight
                },
                showExitDialog = showExitDialog,
                onExitDialogDismiss = { showExitDialog = false },
                onConfirmExit = {
                    if (versionUpdateService.isDownloading) {
                        versionUpdateService.cancelDownload()
                    }
                    exitCleanly(::exitApplication)
                },
                exitApplication = { exitApplication() }
            )


        }
    }
}

@Composable
@Preview
fun App(
    onFatalError: (String) -> Unit = { error(it) },
    onWindowModeChange: (Boolean, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp) -> Unit = { _, _, _ -> },
    showExitDialog: Boolean = false,
    onExitDialogDismiss: () -> Unit = {},
    onConfirmExit: () -> Unit = {},
    exitApplication: () -> Unit = {}
) {
    val service = remember { springContext.getBean<NextGpuAgentService>() }
    val downloadService = remember { springContext.getBean<ModelDownloadService>() }
    val versionUpdateService = remember { springContext.getBean<VersionUpdateService>() }
    val settingsViewModel = remember { SettingsViewModel(service, downloadService, versionUpdateService) }

    val globalConfig = remember { springContext.getBean(GlobalPropertyConfig::class.java) }

    val distro = service.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO).getValue<String>()
    val username = service.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME).getValue<String>()

    // NEW STATE: Control the visibility of the native system password dialog
    var wslStatus by remember { mutableStateOf(false) }
    var showSystemPasswordDialog by remember { mutableStateOf(false) }

    var showEnableWSLDialog by remember { mutableStateOf(false) }

    var showSystemRebootDialog by remember { mutableStateOf(false) }

    // Initial route calculation
    val currentScreen = remember {
        service.autoLogin()

        val setupCompleted = service.getGlobalProperty(GlobalPropertyConfig.IS_SETUP_COMPLETED)


        val screenName = if (!setupCompleted.getValue<Boolean>()) {
            "welcome"
        } else {
            "hub"
        }

        mutableStateOf(screenName)
    }

    LaunchedEffect(currentScreen.value) {
        // Check the status of WSL2
        wslStatus = OSUtil.isWslEnabled()

        if(!wslStatus) {
            showEnableWSLDialog = true
            return@LaunchedEffect
        }

        val isSetupPhase = currentScreen.value == "welcome" || currentScreen.value == "launch"
        if (isSetupPhase) {
            onWindowModeChange(false, 1200.dp, 800.dp)
        } else {
            onWindowModeChange(true, 1280.dp, 720.dp)
        }
    }

    // Trigger WSL startup ONLY when we are out of the setup phase
    LaunchedEffect(currentScreen.value) {
        // Skip checking for the password if we are still setting up the app
        if (currentScreen.value == "welcome" || currentScreen.value == "launch") {
            return@LaunchedEffect
        }
        // Check the status of WSL2
        if(!wslStatus) {
            return@LaunchedEffect
        }

        val storedPassword = service.getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD)?.getValue<String>()
            ?: Paths.get(System.getenv("LOCALAPPDATA"), "NextGPU", "wsl_credentials.txt").let {
                if (Files.exists(it)) Files.readString(it, StandardCharsets.UTF_8).trim() else null
            }

        if (!storedPassword.isNullOrBlank()) {
            globalConfig.updateOsPasswordProperty(storedPassword)
            withContext(Dispatchers.IO) {
                if (OSUtil.authenticateWsl(distro, username, storedPassword)) {
                    val started = OSUtil.ensureWslStarted(distro, username, storedPassword)
                    if (!started) {
                        withContext(Dispatchers.Main) {
                            JOptionPane.showMessageDialog(
                                null,
                                "Failed to start WSL distribution '$distro'. Please ensure WSL is installed and the distribution exists.",
                                "NextGPU – WSL Error",
                                JOptionPane.ERROR_MESSAGE
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        showSystemPasswordDialog = true
                    }
                }
            }
        } else {
            showSystemPasswordDialog = true
        }
    }

    val useDarkTheme = when (settingsViewModel.appThemeMode) {
        AppThemeMode.System -> isSystemInDarkTheme()
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
    }

    // Settings visibility + which tab to land on when opened
    var showSettings by remember { mutableStateOf(false) }
    var settingsInitialTab by remember { mutableStateOf("general") }

    NextGpuTheme(darkTheme = useDarkTheme) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            when (currentScreen.value) {
                "welcome" -> UnifiedSetupScreen(onProceed = { currentScreen.value = "launch" })
                "launch" -> LaunchScreen(onProceed = { currentScreen.value = "hub" })
                "hub" -> HubScreen(
                    onProvider = { currentScreen.value = "provider" },
                    onLogout = { currentScreen.value = "logout" },
                    onModels = { currentScreen.value = "models" },
                    // Accept a tabId so deep links (e.g. from PromptRegion) land on the right tab
                    onSettings = { tabId ->
                        settingsInitialTab = tabId
                        showSettings = true
                    },
                    isPrivateMode = settingsViewModel.isPrivateMode,
                    onPrivateModeChange = { settingsViewModel.updatePrivateMode(it) },
                    viewModel = settingsViewModel,
                )

                "provider" -> ProviderScreen(onReturn = { currentScreen.value = "hub" })
                "logout" -> LogoutScreen(onReturn = { currentScreen.value = "welcome" })
                "nuke" -> NukeScreen(
                    onProceed = { currentScreen.value = "welcome" },
                    onReturn = { currentScreen.value = "hub" })

                "reports" -> ReportsScreen(onReturn = { currentScreen.value = "hub" })
                "models" -> ModelsScreen(onReturn = { currentScreen.value = "settings" })
            }

            // --- NATIVE SYSTEM PASSWORD DIALOG ---
            if (showSystemPasswordDialog) {
                var tempPassword by remember { mutableStateOf("") }
                var passwordVisible by remember { mutableStateOf(false) }
                var isChecking by remember { mutableStateOf(false) }
                var isError by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                Dialog(onDismissRequest = { /* Block dismiss so they must authenticate or cancel */ }) {
                    Surface(
                        shape = RoundedCornerShape(RadiusLarge),
                        color = NextGpuTheme.colors.background,
                        contentColor = NextGpuTheme.colors.textPrimary,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                    ) {
                        Column(modifier = Modifier.padding(SpacingExtraLarge)) {
                            Text(
                                "System Password Required",
                                style = NextGpuTheme.typography.h5,
                                modifier = Modifier.padding(bottom = SpacingSmall)
                            )
                            Text(
                                "Please enter the WSL password for user '$username'. This is required to start the NextGPU services securely.",
                                style = MaterialTheme.typography.body1,
                                color = NextGpuTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.height(SpacingExtraLarge))

                            OutlinedTextField(
                                value = tempPassword,
                                onValueChange = {
                                    tempPassword = it
                                    isError = false // Clear error when they start typing again
                                },
                                shape = RoundedCornerShape(RadiusMedium),
                                singleLine = true,
                                label = { Text("WSL Password") },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    val image =
                                        if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            imageVector = image,
                                            contentDescription = null,
                                            tint = NextGpuTheme.colors.textSecondary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                isError = isError,
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = NextGpuTheme.colors.textPrimary,
                                    unfocusedBorderColor = NextGpuTheme.colors.border,
                                    cursorColor = NextGpuTheme.colors.textPrimary,
                                    errorBorderColor = Color.Red,
                                    focusedLabelColor = NextGpuTheme.colors.textPrimary,
                                    unfocusedLabelColor = NextGpuTheme.colors.textSecondary
                                )
                            )

                            if (isError) {
                                Text(
                                    "Invalid password. Please try again.",
                                    color = Color.Red,
                                    style = MaterialTheme.typography.caption,
                                    modifier = Modifier.padding(top = SpacingSmall)
                                )
                            }

                            Spacer(modifier = Modifier.height(SpacingExtraLarge))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CustomButton(
                                    text = "Cancel",
                                    onClick = {
                                        showSystemPasswordDialog = false
                                        onFatalError("Authentication cancelled. Missing required system privileges.")
                                    },
                                    backgroundColor = Color.Transparent,
                                    textColor = NextGpuTheme.colors.textPrimary,
                                    elevation = false
                                )
                                Spacer(modifier = Modifier.width(SpacingSmall))
                                CustomButton(
                                    text = if (isChecking) "Checking..." else "Authenticate",
                                    enabled = tempPassword.isNotBlank() && !isChecking,
                                    onClick = {
                                        isChecking = true
                                        // Run the heavy WSL command in the background thread
                                        scope.launch(Dispatchers.IO) {
                                            val validCheck = OSUtil.authenticateWsl(distro, username, tempPassword)
                                            withContext(Dispatchers.Main) {
                                                isChecking = false
                                                if (validCheck) {
                                                    globalConfig.updateOsPasswordProperty(tempPassword)
                                                    showSystemPasswordDialog = false
                                                    // Also ensure WSL starts after manual password entry
                                                    scope.launch(Dispatchers.IO) {
                                                        val started =
                                                            OSUtil.ensureWslStarted(distro, username, tempPassword)
                                                        if (!started) {
                                                            withContext(Dispatchers.Main) {
                                                                JOptionPane.showMessageDialog(
                                                                    null,
                                                                    "Failed to start WSL distribution '$distro'. Please ensure WSL is installed and the distribution exists.",
                                                                    "NextGPU – WSL Error",
                                                                    JOptionPane.ERROR_MESSAGE
                                                                )
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    isError = true
                                                }
                                            }
                                        }
                                    },
                                    backgroundColor = NextGpuTheme.colors.primary,
                                    textColor = NextGpuTheme.colors.textPrimary,
                                    disabledBackgroundColor = Color.Transparent,
                                    disabledTextColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f),
                                    borderColor = if (tempPassword.isNotBlank() && !isChecking) Color.Transparent else NextGpuTheme.colors.border,
                                    elevation = false
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            SettingsPopup(
                onDismiss = { showSettings = false },
                currentTheme = settingsViewModel.appThemeMode,
                onThemeChange = { settingsViewModel.updateTheme(it) },
                isPrivateMode = settingsViewModel.isPrivateMode,
                onPrivateModeChange = { settingsViewModel.updatePrivateMode(it) },
                isAdvancedMode = settingsViewModel.isAdvancedMode,
                onAdvancedModeChange = { settingsViewModel.updateAdvancedMode(it) },
                onNavigateToNuke = {
                    showSettings = false
                    currentScreen.value = "nuke"
                },
                initialTabId = settingsInitialTab,
                viewModel = settingsViewModel,
            )
        }

        if (settingsViewModel.showUpdatePopup) {
            Dialog(onDismissRequest = {}) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(modifier = Modifier.padding(SpacingDialog)) {
                        Text(
                            text = if (settingsViewModel.isUpdateDownloading) "Downloading Update" else "Update Available",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = if (settingsViewModel.isUpdateDownloading) "Please wait while the new version of NextGPU is being downloaded. The installer will launch automatically once finished."
                            else "A new version of NextGPU v${settingsViewModel.latestVersionInfo?.version} is available. Would you like to update now?",
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        if (settingsViewModel.isUpdateDownloading) {
                            Spacer(modifier = Modifier.height(SpacingLarge))
                            SimpleProgressBar(
                                progress = settingsViewModel.updateDownloadProgress,
                                modifier = Modifier.fillMaxWidth(),
                                color = NextGpuTheme.colors.primaryVariant,
                                backgroundColor = NextGpuTheme.colors.border
                            )
                            Text(
                                text = "Downloading: ${(settingsViewModel.updateDownloadProgress * 100).toInt()}%",
                                style = NextGpuTheme.typography.caption,
                                modifier = Modifier.padding(top = SpacingSmall)
                            )
                        }

                        settingsViewModel.updateError?.let { error ->
                            Text(
                                text = error,
                                color = Color.Red,
                                style = NextGpuTheme.typography.caption,
                                modifier = Modifier.padding(top = SpacingSmall)
                            )
                        }

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        Row(
                            modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End
                        ) {
                            if (settingsViewModel.isUpdateDownloading) {
                                CustomButton(
                                    text = "Cancel",
                                    onClick = {
                                        settingsViewModel.cancelUpdate();
                                        settingsViewModel.setUpdatePopupVisibility(false)
                                    },
                                    backgroundColor = Color.Transparent,
                                    textColor = NextGpuTheme.colors.textSecondary,
                                    elevation = false
                                )
                            } else {
                                CustomButton(
                                    text = "Later",
                                    onClick = { settingsViewModel.snoozeUpdate() },
                                    backgroundColor = Color.Transparent,
                                    textColor = NextGpuTheme.colors.textSecondary,
                                    elevation = false
                                )

                                Spacer(modifier = Modifier.width(SpacingSmall))

                                CustomButton(
                                    text = "Update Now",
                                    onClick = {
                                        settingsViewModel.startUpdate();
                                        showSettings = false

                                    },
                                    backgroundColor = NextGpuTheme.colors.primary,
                                    textColor = Primary03Black,
                                    elevation = false
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showExitDialog) {
            Dialog(onDismissRequest = onExitDialogDismiss) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog, start = SpacingDialog, end = SpacingDialog, bottom = SpacingLarge
                        )
                    ) {
                        Text(
                            text = "Quit NextGPU?",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        val exitMessage = when {
                            settingsViewModel.isUpdateDownloading && settingsViewModel.downloadingModels.isNotEmpty() ->
                                "Version update and model downloads are in progress. If you quit, the version update will be cancelled, but model downloads will continue in the background."
                            settingsViewModel.isUpdateDownloading ->
                                "A version update is being downloaded. If you quit now, the download will be stopped."
                            else ->
                                "Model downloads continue in the background. If unfinished, click download to resume."
                        }

                        Text(
                            text = exitMessage,
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                text = "Cancel",
                                onClick = onExitDialogDismiss,
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Quit",
                                onClick = onConfirmExit,
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }

        if (showEnableWSLDialog) {
            Dialog(onDismissRequest = onExitDialogDismiss) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog, start = SpacingDialog, end = SpacingDialog, bottom = SpacingLarge
                        )
                    ) {
                        Text(
                            text = "Enable WSL",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "WSL is required to run NextGPU. Please enable WSL and restart the app to continue. The action require system reboot.",
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                text = "Cancel",
                                onClick = {
                                    showEnableWSLDialog = false
                                    exitApplication()
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Enable",
                                onClick = {
                                    OSUtil.enableWsl()
                                    showEnableWSLDialog = false
                                    showSystemRebootDialog = true
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }

        if (showSystemRebootDialog) {
            Dialog(onDismissRequest = onExitDialogDismiss) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog, start = SpacingDialog, end = SpacingDialog, bottom = SpacingLarge
                        )
                    ) {
                        Text(
                            text = "Reboot System",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "To continue using NextGPU, please reboot your system.",
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                text = "Cancel",
                                onClick = exitApplication,
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Reboot",
                                onClick = {
                                    OSUtil.restartSystem()
                                    exitApplication()
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

object SwingPasswordDialog {
    fun askPassword(): String? {
        val passwordField = JPasswordField()
        val panel = JPanel().apply {
            layout = BorderLayout()
            add(
                JLabel("Enter WSL password for user 'nextgpu':"), BorderLayout.NORTH
            )
            add(passwordField, BorderLayout.CENTER)
        }

        val result = JOptionPane.showConfirmDialog(
            null, panel, "NextGPU – System Password Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        )

        return if (result == JOptionPane.OK_OPTION) {
            String(passwordField.password)
        } else {
            null
        }
    }
}