package ai.nextgpu.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.util.BenchmarkUtil
import ai.nextgpu.agent.util.OSUtil
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.ui.component.CustomButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.window.Dialog
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ai.nextgpu.agent.ui.SetupScreen")

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SetupScreen(
    onProceed: () -> Unit,
    onReturn: () -> Unit,
) {
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }
    val scope = rememberCoroutineScope()

    // --- MAPPING DICTIONARY ---
    val stepDescriptions = remember {
        mapOf(
            "init" to "Initializing Installation...",
            "wsl_install" to "Configuring Windows Subsystem for Linux (WSL)",
            "cuda_setup" to "Installing NVIDIA CUDA Drivers & Toolkit",
            "ollama_deploy" to "Deploying Ollama & Pulling DeepSeek-R1",
            "comfyui_deploy" to "Setting up ComfyUI Workspace & Dependencies",
            "finished" to "Base System Installation Complete",
            "hw_scan" to "Analyzing System Hardware...",
            "env_config" to "Configuring Operating Environment...",
            "bench_cpu" to "Benchmarking CPU Performance...",
            "bench_gpu" to "Benchmarking GPU Performance...",
            "bench_mem" to "Benchmarking Memory Speeds...",
            "bench_storage" to "Benchmarking Storage Speeds...",
            "model_scan" to "Scanning Installed AI Models...",
            "setup_done" to "All Systems Configured & Ready."
        )
    }

    // --- SHARED STATES ---
    var elapsedSeconds by remember { mutableStateOf(0) }
    var isLogsExpanded by remember { mutableStateOf(false) }
    val terminalScrollState = rememberScrollState()

    // --- PHASE 1: INSTALLATION STATES ---
    var installState by remember { mutableStateOf(OSUtil.getInstallState()) }
    var isInstalling by remember { mutableStateOf(false) }
    var rawLogs by remember { mutableStateOf("") }

    // Dialog States
    var showOverwriteDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var userPasswordInput by remember { mutableStateOf("") }
    var pendingOverwriteChoice by remember { mutableStateOf(false) }

    val isInstallCompleted = installState.initialized || installState.progressPercentage == 100

    // --- PHASE 2: SETUP STATES ---
    var isSetupRunning by remember { mutableStateOf(false) }
    var setupCompleted by remember { mutableStateOf(false) }
    var currentSetupStepKey by remember { mutableStateOf("hw_scan") }
    var setupLogs by remember { mutableStateOf("> System installation finished.\n> Awaiting manual hardware detection start...") }

    // Reusable install trigger
    val startInstallation = { overwrite: Boolean ->
        isInstalling = true

        // FORCE UI update by explicitly creating a new Java object instance.
        // (Compose ignores updates if you just modify the old instance)
        val optimisticState = OSUtil.InstallState()
        optimisticState.initialized = installState.initialized
        optimisticState.progressPercentage = installState.progressPercentage
        optimisticState.currentStepName = installState.currentStepName
        optimisticState.status = "RUNNING"
        optimisticState.error = null
        installState = optimisticState

        try {
            OSUtil.startPrerequisitesInstallAsync(overwrite)
        } catch (e: Exception) {
            val failedState = OSUtil.InstallState()
            failedState.error = e.message
            failedState.status = "FAILED"
            installState = failedState
            isInstalling = false
        }
    }

    // --- EFFECTS ---

    // NO AUTO-START: Just pause if incomplete
    LaunchedEffect(Unit) {
        if (!isInstallCompleted && installState.progressPercentage in 1..99) {
            installState = installState.apply { status = "PAUSED" }
        }
    }

    // Polling Loop with Grace Period Fix
    LaunchedEffect(isInstalling) {
        if (isInstalling) {
            rawLogs = OSUtil.getInstallLogs() // Immediate fetch before loop

            // Give PowerShell up to 4 seconds (8 loops * 500ms) to overwrite the old FAILED state
            var startupGracePeriod = 8

            while (isInstalling) {
                val newState = OSUtil.getInstallState()
                rawLogs = OSUtil.getInstallLogs()

                // RACE CONDITION PROTECTOR:
                // If we see a FAILED state but we just started, ignore it. It's leftover from the last run.
                if (startupGracePeriod > 0 && newState.status == "FAILED") {
                    startupGracePeriod--
                } else {
                    // It's either a real RUNNING state, or the grace period expired
                    installState = newState

                    // If we successfully read a RUNNING/COMPLETED state, cancel the grace period early
                    if (newState.status != "FAILED") {
                        startupGracePeriod = 0
                    }

                    // Stop loop if finished or if it legitimately failed *after* the grace period
                    if (newState.initialized || newState.status == "FAILED" || newState.status == "COMPLETED") {
                        rawLogs = OSUtil.getInstallLogs() // One last fetch
                        isInstalling = false
                    }
                }
                delay(500)
            }
        }
    }

    // Add this near your other LaunchedEffects
    LaunchedEffect(Unit) {
        // If we are already in a failed or partial state, pull the existing logs immediately
        if (installState.status == "FAILED" || installState.progressPercentage > 0) {
            rawLogs = OSUtil.getInstallLogs()
        }
    }

    LaunchedEffect(isInstalling, isSetupRunning) {
        if (isInstalling || isSetupRunning) {
            while (isInstalling || isSetupRunning) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    LaunchedEffect(rawLogs, setupLogs, isLogsExpanded) {
        if (isLogsExpanded) {
            terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
        }
    }

    val formattedTime = remember(elapsedSeconds) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    val animatedInstallProgress by animateFloatAsState(
        targetValue = installState.progressPercentage / 100f,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val sweepingBias by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    fun appendSetupLog(key: String, logDetail: String? = null) {
        currentSetupStepKey = key
        val desc = stepDescriptions[key] ?: key
        val finalMsg = logDetail ?: desc
        setupLogs += "\n> $finalMsg"
        logger.info("SETUP LOG: {}", finalMsg)
    }

    LaunchedEffect(isSetupRunning) {
        if (isSetupRunning) {
            try {
                withContext(Dispatchers.IO) {
                    appendSetupLog("env_config", "Validating Database Credentials...")
                    delay(500)

                    val distroProp = service.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO)
                    val userProp = service.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME)
                    val passProp = service.getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD)
                    val distro = distroProp.getValueReference()
                    val username = userProp.getValueReference()
                    var password = passProp.getValueReference()

                    if (password.isNullOrBlank()) {
                        val credFile = File(System.getenv("LOCALAPPDATA"), "NextGPU/wsl_credentials.txt")
                        if (credFile.exists()) {
                            password = credFile.readText().trim()
                            // Save it to the DB so we don't have to read the file next time
                            passProp.valueReference = password
                            service.saveGlobalProperty(passProp)
                        }
                    }

                    // RESTART PROTECTION: Ask for password if missing from DB
                    if (distro.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
                        withContext(Dispatchers.Main) {
                            isSetupRunning = false
                            showPasswordDialog = true
                            pendingOverwriteChoice = false
                        }
                        return@withContext
                    }

                    appendSetupLog("hw_scan")
                    delay(1000)
                    val hwReport = service.generateComputerHardwareReport(false)
                    File("reports").mkdirs()
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                    hwReport.exportToHtml("reports/Hardware-Report-$timestamp.html")
                    appendSetupLog("hw_scan", "Hardware detected and report generated.")
                    delay(1000)

                    appendSetupLog("env_config", "Validating AI Agent Containers...")
                    delay(1000)

                    val benchmarkUtil = springContext.getBean(BenchmarkUtil::class.java)

                    appendSetupLog("bench_cpu")
                    val cpu = benchmarkUtil.benchmarkCpu()
                    delay(1000)

                    appendSetupLog("bench_gpu")
                    val gpu = benchmarkUtil.benchmarkGpu()
                    delay(1000)

                    appendSetupLog("bench_mem")
                    val memory = benchmarkUtil.benchmarkMemory(false)
                    delay(1000)

                    appendSetupLog("bench_storage")
                    val storage = benchmarkUtil.benchmarkStorage(false)
                    delay(1000)

                    appendSetupLog("setup_done", "Cleaning up temporary installation files...")
                    OSUtil.deleteInstallCredentials()
                    delay(500)

                    val setupProp = service.getGlobalProperty(GlobalPropertyConfig.IS_SETUP_COMPLETED)
                    setupProp.valueReference = "true"
                    service.saveGlobalProperty(setupProp)


                    delay(500)

                    appendSetupLog("setup_done")
                    withContext(Dispatchers.Main) {
                        setupCompleted = true
                        isSetupRunning = false
                    }
                }
            } catch (e: Exception) {
                logger.error("Setup workflow failed at step {}", currentSetupStepKey, e)
                appendSetupLog(currentSetupStepKey, "Error: ${e.message}")
                withContext(Dispatchers.Main) { isSetupRunning = false }
            }
        }
    }

    // --- DYNAMIC UX STATES ---
    val isPhase2Active = isSetupRunning || setupCompleted
    // Change this line (approx line 236)
    val showProgressAndLogs = installState.progressPercentage > 0 ||
            isInstallCompleted ||
            installState.status == "FAILED" // Add this check

    val titleText = when {
        isPhase2Active -> "Configuring Environment"
        isInstallCompleted -> "Installation Complete"
        else -> "Installing Core Dependencies"
    }

    val subtitleText = when {
        isPhase2Active -> "Setting up hardware profiling and configuring local AI models. Please do not close the application."
        isInstallCompleted -> "Your isolated Linux environment and core drivers are ready. Next, we will analyze your hardware to optimize performance."
        else -> "Downloading core AI models and CUDA drivers. This may take 10-20 minutes depending on your internet connection."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGpuTheme.colors.background)
            .padding(SpacingExtraLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState())
        ) {
            val logoResource = if (NextGpuTheme.colors.isDark) {
                "images/nextgpu-secondary-logo-full.svg"
            } else {
                "images/nextgpu-primary-logo-full.svg"
            }
            Image(
                painter = painterResource(logoResource),
                contentDescription = "NextGPU Logo",
                modifier = Modifier
                    .width(180.dp)
                    .padding(bottom = SpacingSmall),
            )

            val installResource = if (NextGpuTheme.colors.isDark) {
                "images/installation-dark.svg"
            } else {
                "images/installation.svg"
            }
            Image(
                painter = painterResource(installResource),
                contentDescription = "installation image",
                modifier = Modifier.size(192.dp)
            )

            Text(
                text = titleText,
                style = MaterialTheme.typography.h5,
                color = NextGpuTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = SpacingSmall)
            )

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .padding(bottom = SpacingExtraLarge)
            )

            // --- INFO CARD (Pre-Installation Only) ---
            AnimatedVisibility(visible = !showProgressAndLogs) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .background(NextGpuTheme.colors.surface, RoundedCornerShape(RadiusMedium))
                        .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
                        .padding(SpacingLarge),
                    verticalArrangement = Arrangement.spacedBy(SpacingMedium)
                ) {
                    InfoRow("1", "Download & Install", "We will install native Linux dependencies and drivers. Time varies based on your internet speed.")
                    InfoRow("2", "Deploy Core Model", "We will install a fast, lightweight reasoning model (DeepSeek-R1) to get you started. More models will be available later in the Hub.")
                    InfoRow("3", "Hardware Profiling", "NextGPU will benchmark your CPU, GPU, and RAM to optimize local inference.")


                    Divider(color = NextGpuTheme.colors.border)

                    Text(
                        text = "Warning: Please do not close NextGPU or put your computer to sleep during this process.",
                        style = MaterialTheme.typography.caption,
                        color = WarnText,
                        fontWeight = FontWeight.Bold
                    )
                }
            }


            // --- UNIFIED PROGRESS BAR ---
            AnimatedVisibility(visible = showProgressAndLogs) {

                // Create a continuous sweeping offset for the shimmer
                val shimmerOffset by infiniteTransition.animateFloat(
                    initialValue = -1000f,
                    targetValue = 3000f, // Sweep far enough to cover wide screens
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )

                // Create the animated brush (sweeps from primaryVariant -> primary -> primaryVariant)
                val animatedProgressBrush = Brush.linearGradient(
                    colors = listOf(
                        NextGpuTheme.colors.primaryVariant,
                        NextGpuTheme.colors.secondaryVariant, // This creates the glowing highlight
                        NextGpuTheme.colors.primaryVariant
                    ),
                    start = Offset(shimmerOffset, 0f),
                    end = Offset(shimmerOffset + 600f, 0f) // The width of the glowing highlight
                )

                Column(modifier = Modifier.fillMaxWidth(0.80f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (isPhase2Active) "System Configuration" else "Installation Progress",
                            style = MaterialTheme.typography.subtitle2,
                            color = NextGpuTheme.colors.textSecondary
                        )
                        Text(
                            text = when {
                                setupCompleted -> "100%"
                                isSetupRunning -> "In Progress"
                                isInstallCompleted -> "100%"
                                else -> "${installState.progressPercentage}%"
                            },
                            style = MaterialTheme.typography.subtitle2,
                            color = NextGpuTheme.colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .border(
                                width = 1.dp,
                                color = NextGpuTheme.colors.border.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (!isInstallCompleted) {
                            if (animatedInstallProgress > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction = animatedInstallProgress.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        // Apply the animated brush here!
                                        .background(animatedProgressBrush, RoundedCornerShape(50))
                                )
                            }
                        } else if (isSetupRunning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(50)),
                                contentAlignment = BiasAlignment(horizontalBias = sweepingBias, verticalBias = 0f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.25f)
                                        .fillMaxHeight()
                                        // Apply the animated brush to the bouncing block!
                                        .background(animatedProgressBrush, RoundedCornerShape(50))
                                )
                            }
                        } else if (isInstallCompleted && !isPhase2Active) {
                            // Waiting to start hardware detection
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .fillMaxHeight()
                                    // Apply the animated brush here!
                                    .background(animatedProgressBrush, RoundedCornerShape(50))
                            )
                        } else if (setupCompleted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(1f)
                                    .fillMaxHeight()
                                    // Apply the animated brush here!
                                    .background(animatedProgressBrush, RoundedCornerShape(50))
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = SpacingSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        val currentKey = if (isPhase2Active) currentSetupStepKey else {
                            if (installState.currentStepName.isBlank()) "Awaiting start..." else installState.currentStepName
                        }
                        val displayDescription = stepDescriptions[currentKey] ?: currentKey

                        Text(
                            text = displayDescription,
                            style = MaterialTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        AnimatedVisibility(visible = elapsedSeconds > 0) {
                            Text(
                                text = "Elapsed: $formattedTime",
                                style = MaterialTheme.typography.caption,
                                color = NextGpuTheme.colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingMedium))

            // --- TERMINAL ---
            AnimatedVisibility(visible = showProgressAndLogs) {
                Surface(
                    shape = RoundedCornerShape(RadiusSmall),
                    color = NextGpuTheme.colors.surface,
                    border = androidx.compose.foundation.BorderStroke(BorderWidth, NextGpuTheme.colors.border),
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .padding(vertical = SpacingMedium)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isLogsExpanded = !isLogsExpanded }
                                .padding(horizontal = SpacingMedium, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ">_",
                                fontFamily = FontFamily.Monospace,
                                color = NextGpuTheme.colors.primaryVariant,
                                modifier = Modifier.padding(end = SpacingSmall),
                                fontWeight = FontWeight.Bold
                            )

                            val terminalKey = if (isPhase2Active) currentSetupStepKey else {
                                if (installState.currentStepName.isBlank()) "Awaiting start..." else installState.currentStepName
                            }
                            val terminalDesc = stepDescriptions[terminalKey] ?: terminalKey

                            Text(
                                text = terminalDesc,
                                style = MaterialTheme.typography.caption.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = NextGpuTheme.colors.textSecondary,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )

                            Text(
                                text = if (isLogsExpanded) "[-]" else "[+]",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = NextGpuTheme.colors.textSecondary
                            )
                        }

                        AnimatedVisibility(visible = isLogsExpanded) {
                            Column {
                                Divider(color = NextGpuTheme.colors.border, thickness = BorderWidth)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .background(NextGpuTheme.colors.background.copy(alpha = 0.5f))
                                        .verticalScroll(terminalScrollState)
                                        .padding(SpacingMedium)
                                ) {
                                    Text(
                                        text = buildAnnotatedString {
                                            if (isPhase2Active) {
                                                append(setupLogs)
                                            } else {
                                                val displayLogs = rawLogs.ifBlank { "" }

                                                if (displayLogs.isNotBlank()) {
                                                    append(displayLogs)
                                                } else if (installState.status == "FAILED") {
                                                    append("> Installation halted due to an error.\n")
                                                } else {
                                                    append("Waiting for process to start...")
                                                }

                                                // Always append the specific error if it's failed, regardless of rawLogs content
                                                if (installState.status == "FAILED" && !installState.error.isNullOrBlank()) {
                                                    withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                                        append("\n\n>>> CRITICAL ERROR: ${installState.error}")
                                                        append("\n>>> You can 'Retry' to continue from the last successful step.")
                                                    }
                                                }
                                            }
                                        }, style = MaterialTheme.typography.caption.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 16.sp
                                        ),
                                        color = NextGpuTheme.colors.primaryVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            // --- ACTION BUTTONS ---
            Box(
                modifier = Modifier.fillMaxWidth().height(50.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!isInstallCompleted) {
                    val isFailed = installState.status == "FAILED"
                    val hasStarted = installState.progressPercentage > 0

                    val buttonText = when {
                        isInstalling -> "Installing..."
                        isFailed -> "Retry Installation"
                        hasStarted -> "Continue Installation"
                        else -> "Start Installation"
                    }

                    CustomButton(
                        text = buttonText,
                        enabled = !isInstalling,
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                        onClick = {
                            scope.launch {
                                // If it's a brand new install, check for existing WSL instance
                                if (!hasStarted && OSUtil.checkIfNextGpuExists()) {
                                    showOverwriteDialog = true
                                }
                                // If we have credentials, just resume/start
                                else if (OSUtil.hasInstallCredentials()) {
                                    startInstallation(false) // false = resume/don't overwrite
                                }
                                // Otherwise, get the password first
                                else {
                                    pendingOverwriteChoice = false
                                    showPasswordDialog = true
                                }
                            }
                        },
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,
                        disabledBackgroundColor = NextGpuTheme.colors.primary.copy(alpha = 0.5f)
                    )

                }else if (!isSetupRunning && !setupCompleted) {
                    // --- PHASE 2 BUTTON ---
                    CustomButton(
                        text = "Start Hardware Detection",
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                        onClick = {
                            isSetupRunning = true
                        },
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black
                    )

                } else if (setupCompleted) {
                    // --- PHASE 3 BUTTON ---
                    CustomButton(
                        text = "Select Use Case",
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                        onClick = {
                            onProceed() // DB flag is already set during "setup_done" step
                        },
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,
                    )
                } else {
                    // --- LOADING STATE ---
                    CustomButton(
                        text = "Configuring...",
                        enabled = false,
                        onClick = { },
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,
                        disabledBackgroundColor = NextGpuTheme.colors.primary.copy(alpha = 0.5f)
                    )
                }
            }
        }

        // --- OVERWRITE DIALOG ---
        if (showOverwriteDialog) {
            Dialog(onDismissRequest = { showOverwriteDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier
                            // Apply uniform padding to top and sides
                            // Reduce bottom padding because buttons have their own internal height
                            .padding(
                                top = SpacingDialog,
                                start = SpacingDialog,
                                end = SpacingDialog,
                                bottom = SpacingLarge// Tighter bottom to balance the button height
                            )
                    ) {
                        Text(
                            text = "Resume Previous Setup?",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "We detected an existing NextGPU environment. You can continue from where you left off, or wipe it and start a completely fresh installation.",
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
                                text = "Start Fresh",
                                onClick = {
                                    showOverwriteDialog = false
                                    pendingOverwriteChoice = true
                                    if (!OSUtil.hasInstallCredentials()) {
                                        showPasswordDialog = true
                                    } else {
                                        startInstallation(true)
                                    }
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                // Use zero vertical padding for the transparent button to align text baseline
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Continue Installation",
                                onClick = {
                                    showOverwriteDialog = false
                                    pendingOverwriteChoice = false
                                    if (!OSUtil.hasInstallCredentials()) {
                                        showPasswordDialog = true
                                    } else {
                                        startInstallation(false)
                                    }
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                elevation = false,
                                // Keep the vertical padding here for the "pill" look
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }
        // --- PASSWORD DIALOG ---
        if (showPasswordDialog) {
            var passwordVisible by remember { mutableStateOf(false) }

            val hasMinLength = userPasswordInput.length >= 8
            val hasUpper = userPasswordInput.any { it.isUpperCase() }
            val hasLower = userPasswordInput.any { it.isLowerCase() }
            val hasDigit = userPasswordInput.any { it.isDigit() }
            val hasSpecial = userPasswordInput.any { "!@#$%^&*".contains(it) }

            val isPasswordValid = hasMinLength && hasUpper && hasLower && hasDigit && hasSpecial

            Dialog(onDismissRequest = { showPasswordDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog,
                            start = SpacingDialog,
                            end = SpacingDialog,
                            bottom = SpacingLarge// Adjusted to balance button height
                        )
                    ) {
                        // TITLE
                        Text(
                            text = "Set Secure Password",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        // BODY DESCRIPTION
                        Text(
                            text = buildAnnotatedString {
                                append("Please create a password for your isolated Linux (WSL) environment. You will need this for administrative tasks inside the containers.\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Medium, color = WarnText)) {
                                    append("Note: ")
                                    append("Please store this password securely. You will not be able to recover it if it is lost.")
                                }
                            },
                            color = NextGpuTheme.colors.textSecondary,
                            style = MaterialTheme.typography.body2
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        // PASSWORD INPUT
                        OutlinedTextField(
                            value = userPasswordInput,
                            onValueChange = { userPasswordInput = it },
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(imageVector = image, contentDescription = null, tint = NextGpuTheme.colors.textSecondary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = userPasswordInput.isNotEmpty() && !isPasswordValid,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = NextGpuTheme.colors.textPrimary,
                                focusedLabelColor = NextGpuTheme.colors.textPrimary,
                                cursorColor = NextGpuTheme.colors.textPrimary,
                                errorBorderColor = Color.Red,
                                errorLabelColor = Color.Red
                            )
                        )

                        // ERROR MESSAGE
                        AnimatedVisibility(visible = userPasswordInput.isNotEmpty() && !isPasswordValid) {
                            Text(
                                text = "Must be at least 8 characters and include uppercase, lowercase, numbers, and a special character (!@#$%^&*).",
                                color = Color.Red,
                                style = MaterialTheme.typography.caption,
                                modifier = Modifier.padding(top = SpacingSmall)
                            )
                        }

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        // ACTION BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CANCEL
                            CustomButton(
                                text = "Cancel",
                                onClick = { showPasswordDialog = false },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            // SAVE & CONTINUE/INSTALL
                            CustomButton(
                                text = if (isInstallCompleted) "Save & Continue" else "Save & Install",
                                enabled = isPasswordValid,
                                onClick = {
                                    if (isPasswordValid) {
                                        showPasswordDialog = false
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                OSUtil.saveInstallCredentials(userPasswordInput)
                                                
                                                val passProp = service.getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD)
                                                passProp.valueReference = userPasswordInput
                                                service.saveGlobalProperty(passProp)

                                                withContext(Dispatchers.Main) {
                                                    if (isInstallCompleted) isSetupRunning = true
                                                    else startInstallation(pendingOverwriteChoice)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    installState = installState.apply {
                                                        error = "Failed to save credentials: ${e.message}"
                                                        status = "FAILED"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLE FOR PRE-INSTALL INFO CARD ---
@Composable
fun InfoRow(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(NextGpuTheme.colors.primaryVariant.copy(alpha = 0.2f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.caption,
                color = NextGpuTheme.colors.primaryVariant,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(SpacingMedium))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle2,
                color = NextGpuTheme.colors.textPrimary
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.caption,
                color = NextGpuTheme.colors.textSecondary
            )
        }
    }
}
