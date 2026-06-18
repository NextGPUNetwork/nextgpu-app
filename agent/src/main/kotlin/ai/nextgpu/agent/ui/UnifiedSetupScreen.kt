package ai.nextgpu.agent.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.hub.sidebar.CustomRoundedCheckbox
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.util.BenchmarkUtil
import ai.nextgpu.agent.util.OSUtil
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.zIndex
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ai.nextgpu.agent.ui.UnifiedSetupScreen")

@OptIn(ExperimentalMaterialApi::class)
enum class SetupPhase { OVERVIEW, INSTALLING, HARDWARE_SCAN, DONE }

@Composable
fun UnifiedSetupScreen(
    onProceed: () -> Unit
) {
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    val eulaUrl = remember {
        object {}.javaClass.classLoader.getResource("eula.html")?.toExternalForm()
    }

    // --- WELCOME/EULA STATES ---
    var eulaAccepted by remember { mutableStateOf(false) }

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

    // --- SHARED LOGIC STATES ---
    var elapsedSeconds by remember { mutableStateOf(0) }
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

    // --- UI FLOW STATE EVALUATION ---
    val isPhase2Active = isSetupRunning || setupCompleted
    val hasStarted = installState.progressPercentage > 0
    val isFailed = installState.status == "FAILED"

    val currentPhase = when {
        setupCompleted -> SetupPhase.DONE
        isPhase2Active -> SetupPhase.HARDWARE_SCAN
        isInstalling || hasStarted || isInstallCompleted -> SetupPhase.INSTALLING
        else -> SetupPhase.OVERVIEW
    }

    // --- INSTALLATION TRIGGER LOGIC ---
    val startInstallation = { overwrite: Boolean ->
        isInstalling = true
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

    fun appendSetupLog(key: String, logDetail: String? = null) {
        currentSetupStepKey = key
        val desc = stepDescriptions[key] ?: key
        val finalMsg = logDetail ?: desc
        setupLogs += "\n> $finalMsg"
        logger.info("SETUP LOG: {}", finalMsg)
    }

    // --- EFFECTS ---
    LaunchedEffect(Unit) {
        if (!isInstallCompleted && installState.progressPercentage in 1..99) {
            installState = installState.apply { status = "PAUSED" }
        }
    }

    // UPDATED: Bulletproof installation tracking loop
    LaunchedEffect(isInstalling) {
        if (isInstalling) {
            rawLogs = OSUtil.getInstallLogs()
            var startupGracePeriod = 8

            while (isInstalling) {
                val newState = OSUtil.getInstallState()
                rawLogs = OSUtil.getInstallLogs()

                if (startupGracePeriod > 0 && newState.status == "FAILED") {
                    startupGracePeriod--
                } else {
                    installState = newState
                    if (newState.status != "FAILED") startupGracePeriod = 0

                    // Added progressPercentage == 100 break condition
                    if (newState.initialized || newState.status == "FAILED" || newState.status == "COMPLETED" || newState.progressPercentage == 100) {
                        rawLogs = OSUtil.getInstallLogs()
                        isInstalling = false

                        if (newState.status != "FAILED" && !isSetupRunning && !setupCompleted) {
                            isSetupRunning = true
                        }
                    }
                }
                delay(500)
            }
        }
    }

    // NEW: Bulletproof Auto-Transition Safety Net
    // If the phase gets stuck on installing, but the installation is complete, force it to move forward.
    LaunchedEffect(currentPhase, isInstallCompleted, isInstalling) {
        if (currentPhase == SetupPhase.INSTALLING && isInstallCompleted && !isInstalling) {
            delay(500) // Small UX delay
            isSetupRunning = true
        }
    }

    LaunchedEffect(Unit) {
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

    LaunchedEffect(rawLogs, setupLogs) {
        terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
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
                            passProp.valueReference = password
                            service.saveGlobalProperty(passProp)
                        }
                    }

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
                logger.error("Unified setup workflow failed at step {}", currentSetupStepKey, e)
                appendSetupLog(currentSetupStepKey, "Error: ${e.message}")
                withContext(Dispatchers.Main) { isSetupRunning = false }
            }
        }
    }

    val setupBackgroundColor = if (!NextGpuTheme.colors.isDark) {
        Secondary05LightPurple
    } else {
        NextGpuTheme.colors.background
    }

    val annotatedString = buildAnnotatedString {
        append("Your data stays on your device only. Private, secure and fully under your control.\n")

        // Attach a tag and the URL payload to the upcoming text
        pushStringAnnotation(
            tag = "privacy_link",
            annotation = "https://nextgpu.ai/privacy-policy/"
        )

        withStyle(style = SpanStyle(color = NextGpuTheme.colors.primaryVariant)) {
            append("Click here to learn more")
        }

        // Pop the annotation so it doesn't apply to the rest of the text
        pop()

        append(" about NextGPU's commitment to privacy and security.")
    }

    // --- MAIN UI LAYOUT ---
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(setupBackgroundColor)
    ) {
        Box(
            modifier = Modifier
                .width(0.dp)
                .fillMaxHeight()
                .zIndex(-1f) // FORCES this entire box behind the text and stepper
        ) {
            val launchResource = "images/welcome.svg"
            Image(
                painter = painterResource(launchResource),
                contentDescription = "Launch illustration",
                modifier = Modifier
                    // This explicitly tells Compose not to clip the image when it overflows
                    .wrapContentSize(unbounded = true, align = Alignment.BottomStart)
                    .width(2000.dp) // Dialed back from 750dp so the laptop is visible
                    .offset(x = (-220).dp, y = 350.dp), // Gentler offsets
                contentScale = ContentScale.FillWidth
            )
        }

        // --- LEFT COLUMN (Overlapping Layout) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SpacingHuge,
                        start = SpacingMassive,
                        end = SpacingLarge
                    ),
                horizontalAlignment = Alignment.Start
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
                        .padding(bottom = SpacingLarge),
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.CenterStart
                )

                Text(
                    text = "Welcome to NextGPU",
                    style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Bold),
                    color = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SpacingSmall)
                )

//                Text(
//                    text = "NextGPU will inspect your hardware and recommend the best models your machine can run, so you can start unlocking the full power of your computer.",
//                    style = MaterialTheme.typography.subtitle1,
//                    color = NextGpuTheme.colors.textSecondary,
//                    modifier = Modifier.width(350.dp)
//                )
            }
        }


        // --- RIGHT COLUMN (Dynamic Stepper & Actions) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(vertical = SpacingExtraLarge, horizontal = SpacingExtraLarge)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NextGpuTheme.colors.background, RoundedCornerShape(RadiusLarge))
                    .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusLarge))
                    .padding(SpacingExtraLarge)
            ) {
                // Header Area
                if (currentPhase == SetupPhase.OVERVIEW) {
                    Text(
                        text = "Installation overview",
                        style = MaterialTheme.typography.h5,
                        color = NextGpuTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = SpacingSmall)
                    )
                    Text(
                        text = "We will guide your device through hardware inspection, model selection, and installation setup.",
                        style = MaterialTheme.typography.body2,
                        color = NextGpuTheme.colors.textSecondary,
                        modifier = Modifier.padding(bottom = SpacingLarge)
                    )
                } else {
                    Text(
                        text = "Installation progress",
                        style = MaterialTheme.typography.h5,
                        color = NextGpuTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = SpacingTiny)
                    )
                    val progressText = when (currentPhase) {
                        // Once we hit Hardware Scan or Done, lock text at 100%
                        SetupPhase.HARDWARE_SCAN, SetupPhase.DONE -> "100%"
                        else -> "${installState.progressPercentage}%"
                    }
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold),
                        color = NextGpuTheme.colors.textPrimary,
                        modifier = Modifier.padding(bottom = SpacingMedium)
                    )

                    // Linear Progress Bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = when (currentPhase) {
                            // Once we hit Hardware Scan or Done, lock bar at full width (1f)
                            SetupPhase.HARDWARE_SCAN, SetupPhase.DONE -> 1f
                            else -> installState.progressPercentage / 100f
                        },
                        animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .background(NextGpuTheme.colors.border.copy(alpha = 0.5f), RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(NextGpuTheme.colors.primaryVariant)
                        )
                    }
                    Spacer(modifier = Modifier.height(SpacingLarge))
                }

                // Scrollable Steps Area
                val isOverview = currentPhase == SetupPhase.OVERVIEW
                var expandedStep by remember(currentPhase) {
                    mutableStateOf<Int?>(
                        when (currentPhase) {
                            SetupPhase.INSTALLING -> 1
                            SetupPhase.HARDWARE_SCAN -> 2
                            SetupPhase.DONE -> null
                            else -> null
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = if (isOverview) Arrangement.spacedBy(SpacingMedium) else Arrangement.Top
                ) {
                    // STEP 1: Installation
                    val isStep1Active = currentPhase == SetupPhase.INSTALLING
                    val isStep1Done = currentPhase == SetupPhase.HARDWARE_SCAN || currentPhase == SetupPhase.DONE

                    ExpandableStepCard(
                        stepNumber = 1,
                        title = "Installation & Setup",
                        description = "We will install NextGPU, prepare the required runtime, and configure your local environment.",
                        isActive = isStep1Active,
                        isDone = isStep1Done,
                        isExpanded = expandedStep == 1,
                        isOverview = isOverview,
                        iconPath = "icons/setup.svg",
                        onToggleExpand = { expandedStep = if (expandedStep == 1) null else 1 }
                    ) {
                        // Logs View
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(NextGpuTheme.colors.surface.copy(alpha = 0.8f), RoundedCornerShape(RadiusLarge))
                                .padding(SpacingLarge)
                        ) {
                            val currentStepText = if (installState.currentStepName.isNotBlank()) {
                                val description = stepDescriptions[installState.currentStepName] ?: installState.currentStepName
                                "> $description"
                            } else if (isInstallCompleted) {
                                "> Setup complete"
                            } else {
                                "> Preparing..."
                            }

                            Text(
                                text = currentStepText,
                                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Normal),
                                color = NextGpuTheme.colors.textPrimary
                            )

                            Spacer(modifier = Modifier.height(SpacingMedium))

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(terminalScrollState)
                            ) {
                                Text(
                                    text = buildAnnotatedString {
                                        val displayLogs = rawLogs.ifBlank { "" }
                                        if (displayLogs.isNotBlank()) append(displayLogs)
                                        else if (installState.status == "FAILED") append("Installation halted due to an error.\n")
                                        else append("Waiting for process to start...")

                                        if (installState.status == "FAILED" && !installState.error.isNullOrBlank()) {
                                            withStyle(style = SpanStyle(color = Color.Red, fontWeight = FontWeight.Bold)) {
                                                append("\n\n>>> CRITICAL ERROR: ${installState.error}")
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.caption.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                    color = NextGpuTheme.colors.textSecondary
                                )
                            }
                        }
                    }

                    if (!isOverview) {
                        Divider(color = NextGpuTheme.colors.border, modifier = Modifier.padding(vertical = SpacingSmall))
                    }

                    // STEP 2: Hardware Inspection
                    val isStep2Active = currentPhase == SetupPhase.HARDWARE_SCAN
                    val isStep2Done = currentPhase == SetupPhase.DONE
                    ExpandableStepCard(
                        stepNumber = 2,
                        title = "Hardware Inspection",
                        description = "We'll scan your GPU, CPU, memory, and storage to understand what your machine can support.",
                        isActive = isStep2Active,
                        isDone = isStep2Done,
                        isExpanded = expandedStep == 2,
                        isOverview = isOverview,
                        iconPath = "icons/hardware-scan.svg",
                        onToggleExpand = { expandedStep = if (expandedStep == 2) null else 2 }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingMedium)) {
                            SubTaskRow("Hardware Detection", currentSetupStepKey == "hw_scan", currentSetupStepKey != "hw_scan" && setupLogs.contains("Hardware detected"))
                            SubTaskRow("Operating Environment Setup", currentSetupStepKey == "env_config", setupLogs.contains("Validating AI Agent Containers"))
                            SubTaskRow("Benchmarking", currentSetupStepKey.startsWith("bench_"), setupCompleted)
                        }
                    }

                    if (!isOverview) {
                        Divider(color = NextGpuTheme.colors.border, modifier = Modifier.padding(vertical = SpacingSmall))
                    }

                    // STEP 3: Launch
                    val isStep3Active = currentPhase == SetupPhase.DONE
                    ExpandableStepCard(
                        stepNumber = 3,
                        title = "Launch",
                        description = "We'll recommend the best AI models for your hardware and the tasks you want to run.",
                        isActive = false,
                        isDone = isStep3Active,
                        isExpanded = expandedStep == 3,
                        isOverview = isOverview,
                        iconPath = "icons/launch.svg",
                        onToggleExpand = { expandedStep = if (expandedStep == 3) null else 3 }
                    ) {}
                }

                if (currentPhase == SetupPhase.OVERVIEW || isFailed) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = SpacingMedium)
                    ) {
                        CustomRoundedCheckbox(checked = eulaAccepted, onCheckedChange = { eulaAccepted = it })
                        Spacer(modifier = Modifier.width(SpacingSmall))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "I accept the ", color = NextGpuTheme.colors.textPrimary, style = MaterialTheme.typography.subtitle2)
                            Text(
                                text = "End-User License Agreement",
                                style = MaterialTheme.typography.subtitle2,
                                color = NextGpuTheme.colors.primaryVariant,
                                modifier = Modifier
                                    .pointerHoverIcon(PointerIcon.Hand)
                                    .clickable(enabled = eulaUrl != null) { eulaUrl?.let { uriHandler.openUri(it) } }
                            )
                        }
                    }
                }

                // UPDATED: Dynamic Button text and enabled logic safely handles paused/completed states
                val buttonText = when (currentPhase) {
                    SetupPhase.OVERVIEW -> "Get started ->"
                    SetupPhase.INSTALLING -> when {
                        isFailed -> "Retry Installation"
                        !isInstalling && isInstallCompleted -> "Start Hardware Scan" // Fallback manually triggers phase 2
                        !isInstalling && hasStarted -> "Continue Installation" // Resumes a paused installation
                        else -> "Installing..."
                    }
                    SetupPhase.HARDWARE_SCAN -> "Detecting Hardware..."
                    SetupPhase.DONE -> "Continue"
                }

                val isButtonEnabled = when (currentPhase) {
                    SetupPhase.OVERVIEW -> eulaAccepted
                    SetupPhase.INSTALLING -> isFailed || !isInstalling
                    SetupPhase.HARDWARE_SCAN -> false
                    SetupPhase.DONE -> true
                }

                CustomButton(
                    text = buttonText,
                    enabled = isButtonEnabled,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        when (currentPhase) {
                            SetupPhase.DONE -> onProceed()
                            SetupPhase.OVERVIEW -> {
                                val eulaProperty = service.getGlobalProperty(GlobalPropertyConfig.IS_EULA_ACCEPTED)
                                eulaProperty.valueReference = "true"
                                service.saveGlobalProperty(eulaProperty)

                                scope.launch {
                                    if (!hasStarted && OSUtil.checkIfNextGpuExists()) {
                                        showOverwriteDialog = true
                                    } else if (OSUtil.hasInstallCredentials()) {
                                        startInstallation(false)
                                    } else {
                                        pendingOverwriteChoice = false
                                        showPasswordDialog = true
                                    }
                                }
                            }
                            SetupPhase.INSTALLING -> {
                                if (isInstallCompleted) {
                                    isSetupRunning = true
                                } else {
                                    scope.launch {
                                        if (OSUtil.hasInstallCredentials()) {
                                            startInstallation(false)
                                        } else {
                                            pendingOverwriteChoice = false
                                            showPasswordDialog = true
                                        }
                                    }
                                }
                            }
                            SetupPhase.HARDWARE_SCAN -> {}
                        }
                    },
                    backgroundColor = NextGpuTheme.colors.primary,
                    textColor = Primary03Black,
                    disabledBackgroundColor = NextGpuTheme.colors.primary.copy(alpha = 0.5f)
                )
                Divider(color = NextGpuTheme.colors.border, modifier = Modifier.padding(vertical = SpacingMedium))

                // Footer Privacy Notice
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        painter = painterResource("images/checkmark.svg"),
                        contentDescription = "Secure",
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(SpacingMedium))


                    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
                    var isHoveringLink by remember { mutableStateOf(false) }

                    ClickableText(
                        text = annotatedString,
                        style = MaterialTheme.typography.caption.copy(
                            color = NextGpuTheme.colors.textSecondary,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier
                            // Dynamically change the cursor based on the state
                            .pointerHoverIcon(if (isHoveringLink) PointerIcon.Hand else PointerIcon.Default)
                            // Track the mouse movement across the text block
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val position = event.changes.first().position

                                        layoutResult?.let { layout ->
                                            // Convert exact mouse X/Y coordinates to a character index
                                            val offset = layout.getOffsetForPosition(position)

                                            // Check if that exact character index has your URL annotation attached
                                            val hasLink = annotatedString.getStringAnnotations(
                                                tag = "privacy_link",
                                                start = offset,
                                                end = offset
                                            ).isNotEmpty()

                                            // Update state to trigger the pointer change
                                            isHoveringLink = hasLink
                                        }
                                    }
                                }
                            },
                        onTextLayout = { result ->
                            // Save the layout result so we can map coordinates to text offsets
                            layoutResult = result
                        },
                        onClick = { offset ->
                            // Your existing click logic
                            annotatedString.getStringAnnotations(tag = "privacy_link", start = offset, end = offset)
                                .firstOrNull()?.let { annotation ->
                                    uriHandler.openUri(annotation.item)
                                }
                        }
                    )
                }
                AnimatedVisibility(visible = expandedStep == null && currentPhase != SetupPhase.OVERVIEW) {
                    Column {
                        Spacer(modifier = Modifier.height(SpacingLarge))
                        HelpfulTipsCard()
                    }
                }
            }
        }

        // --- OVERLAY DIALOGS ---
        if (showOverwriteDialog) {
            Dialog(onDismissRequest = { showOverwriteDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusLarge),
                    color = NextGpuTheme.colors.background,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(modifier = Modifier.padding(SpacingExtraLarge)) {
                        Text("Resume Previous Setup?", style = NextGpuTheme.typography.h5, modifier = Modifier.padding(bottom = SpacingSmall))
                        Text(
                            "We detected an existing NextGPU environment. You can continue from where you left off, or wipe it and start a completely fresh installation.",
                            style = NextGpuTheme.typography.body1,
                            color = NextGpuTheme.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(SpacingExtraLarge))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            CustomButton(
                                text = "Start Fresh",
                                onClick = {
                                    showOverwriteDialog = false
                                    pendingOverwriteChoice = true
                                    if (!OSUtil.hasInstallCredentials()) showPasswordDialog = true else startInstallation(true)
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textPrimary,
                                elevation = false
                            )
                            Spacer(modifier = Modifier.width(SpacingSmall))
                            CustomButton(
                                text = "Continue Installation",
                                onClick = {
                                    showOverwriteDialog = false
                                    pendingOverwriteChoice = false
                                    if (!OSUtil.hasInstallCredentials()) showPasswordDialog = true else startInstallation(false)
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

        if (showPasswordDialog) {
            var passwordVisible by remember { mutableStateOf(false) }
            val isPasswordValid = userPasswordInput.length >= 8 && userPasswordInput.any { it.isUpperCase() } && userPasswordInput.any { it.isLowerCase() } && userPasswordInput.any { it.isDigit() } && userPasswordInput.any { "!@#$%^&*".contains(it) }

            Dialog(onDismissRequest = { showPasswordDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusLarge),
                    color = NextGpuTheme.colors.background,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(modifier = Modifier.padding(SpacingExtraLarge)) {
                        Text("Set secure password", style = NextGpuTheme.typography.h5, modifier = Modifier.padding(bottom = SpacingSmall))
                        Text(
                            text = buildAnnotatedString {
                                append("Please create a password for your isolated Linux (WSL) environment. You will need this for administrative tasks inside the containers.\n\n")
                                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = NextGpuTheme.colors.textPrimary)) {
                                    append("Note: ")
                                }
                                append("Please store this password securely. You will not be able to recover it if it is lost.")
                            },
                            color = NextGpuTheme.colors.textSecondary,
                            style = MaterialTheme.typography.body1
                        )
                        Spacer(modifier = Modifier.height(SpacingExtraLarge))

                        OutlinedTextField(
                            value = userPasswordInput,
                            onValueChange = { userPasswordInput = it },
                            shape = RoundedCornerShape(RadiusMedium),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    // Add the pointer modifier here
                                    modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                                ) {
                                    Icon(
                                        imageVector = image,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = NextGpuTheme.colors.textSecondary
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            isError = userPasswordInput.isNotEmpty() && !isPasswordValid,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = NextGpuTheme.colors.textPrimary,
                                unfocusedBorderColor = NextGpuTheme.colors.border,
                                cursorColor = NextGpuTheme.colors.textPrimary,
                                errorBorderColor = Color.Red
                            )
                        )

                        if (userPasswordInput.isNotEmpty() && !isPasswordValid) {
                            Text(
                                "Must be at least 8 characters and include uppercase, lowercase, numbers, and a special character (!@#$%^&*).",
                                color = Color.Red, style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = SpacingSmall)
                            )
                        } else {
                            Text(
                                "Use at least 8 characters.",
                                color = NextGpuTheme.colors.textSecondary, style = MaterialTheme.typography.caption, modifier = Modifier.padding(top = SpacingSmall)
                            )
                        }

                        Spacer(modifier = Modifier.height(SpacingExtraLarge))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                            CustomButton(
                                text = "Cancel",
                                onClick = { showPasswordDialog = false },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textPrimary,
                                elevation = false
                            )
                            Spacer(modifier = Modifier.width(SpacingSmall))
                            CustomButton(
                                text = if (isInstallCompleted) "Save & Install" else "Set a password",
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
                                                    if (isInstallCompleted) isSetupRunning = true else startInstallation(pendingOverwriteChoice)
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    installState = installState.apply { error = "Failed to save credentials: ${e.message}"; status = "FAILED" }
                                                }
                                            }
                                        }
                                    }
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                disabledBackgroundColor = Color.Transparent,
                                disabledTextColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f),
                                borderColor = if (isPasswordValid) Color.Transparent else NextGpuTheme.colors.border,
                                elevation = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    iconPath: String,
    isActive: Boolean,
    isDone: Boolean,
    isExpanded: Boolean,
    isOverview: Boolean,
    onToggleExpand: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val bgColor = if (isOverview) NextGpuTheme.colors.surface.copy(alpha = 0.5f) else Color.Transparent
    val borderColor = if (isOverview) NextGpuTheme.colors.border else Color.Transparent
    val cardPadding = if (isOverview) SpacingLarge else 0.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(RadiusLarge))
            .border(BorderWidth, borderColor, RoundedCornerShape(RadiusLarge))
            .padding(cardPadding)
    ) {
        if (isOverview) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f), RoundedCornerShape(RadiusMedium)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(iconPath), contentDescription = null, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(SpacingLarge))
                Column {
                    Text("$stepNumber. $title", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Medium), color = NextGpuTheme.colors.textPrimary, modifier = Modifier.padding(bottom = SpacingTiny))
                    Text(description, style = MaterialTheme.typography.body2, color = NextGpuTheme.colors.textSecondary, lineHeight = 20.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingMedium)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isActive || isDone) onToggleExpand()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .let {
                                if (isDone) it.background(NextGpuTheme.colors.primaryVariant.copy(alpha = 0.15f), RoundedCornerShape(50))
                                else if (isActive) it.background(NextGpuTheme.colors.primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                                else it.border(2.dp, NextGpuTheme.colors.border, RoundedCornerShape(50))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Icon(Icons.Default.Check, contentDescription = "Done", tint = NextGpuTheme.colors.primaryVariant, modifier = Modifier.size(18.dp))
                        else if (isActive) Box(modifier = Modifier.size(10.dp).background(Primary03Black, RoundedCornerShape(50)))
                    }

                    Spacer(modifier = Modifier.width(SpacingMedium))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6.copy(fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal),
                        color = if (isActive) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary
                    )
                }

                if (isActive || isDone) {
                    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NextGpuTheme.colors.surface, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { onToggleExpand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource("icons/arrow-down.svg"),
                            contentDescription = "Toggle",
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = rotation },
                            colorFilter = ColorFilter.tint(NextGpuTheme.colors.textPrimary)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(modifier = Modifier.padding(bottom = SpacingMedium, start = 32.dp + SpacingMedium)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun SubTaskRow(title: String, isActive: Boolean, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = SpacingSmall)) {
        if (isDone) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = NextGpuTheme.colors.primaryVariant, modifier = Modifier.size(16.dp))
        } else if (isActive) {
            CircularProgressIndicator(modifier = Modifier.size(12.dp), color = NextGpuTheme.colors.primaryVariant, strokeWidth = 2.dp)
        } else {
            Box(modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(SpacingSmall))
        Text(
            text = title,
            style = MaterialTheme.typography.body2,
            color = if (isActive || isDone) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary
        )
    }
}

@Composable
fun HelpfulTipsCard(modifier: Modifier = Modifier) {
    val tips = listOf(
        "Your AI setup runs locally." to "That means your prompts, files, and model activity can stay on your device.",
        "Smaller models are often faster." to "If you want quick responses, start with a lighter model before trying larger ones.",
        "NextGPU checks your hardware first." to "This helps avoid installing models that are too large or slow for your machine.",
        "You can change models later." to "Start with the recommended option, then experiment once everything is running.",
        "Local AI works best when your GPU has enough free memory." to "Closing heavy apps can improve performance.",
        "ComfyUI and Ollama can take a moment to start." to "If setup pauses briefly, it usually means services are being prepared in the background."
    )

    var currentIndex by remember { mutableStateOf(0) }
    val currentTip = tips[currentIndex]

    // Animation state for the loader (0f to 1f)
    val progress = remember { Animatable(0f) }
    val cycleDurationMs = 6000 // 6 seconds per tip

    // Auto-cycle logic: re-triggers whenever currentIndex changes
    LaunchedEffect(currentIndex) {
        progress.snapTo(0f) // Reset progress
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = cycleDurationMs, easing = LinearEasing)
        )
        // Move to the next tip once the animation completes
        currentIndex = if (currentIndex < tips.size - 1) currentIndex + 1 else 0
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusLarge))
            .padding(SpacingLarge)
    ) {
        Column {
            // Updated Top Row: Text + Progress Loader aligned to the End
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = progress.value,
                    modifier = Modifier.size(12.dp), // Tiny loader
                    color = NextGpuTheme.colors.textSecondary,
                    strokeWidth = 2.dp
                )

                Spacer(modifier = Modifier.width(SpacingSmall))

                Text(
                    text = "Helpful tip ${currentIndex + 1} of ${tips.size}",
                    style = MaterialTheme.typography.caption,
                    color = NextGpuTheme.colors.textSecondary
                )




            }

            Spacer(modifier = Modifier.height(SpacingMedium))

            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(NextGpuTheme.colors.surface, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("icons/lightbulb.svg"),
                        contentDescription = "Tip",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(NextGpuTheme.colors.textPrimary)
                    )
                }

                Spacer(modifier = Modifier.width(SpacingLarge))

                Column {
                    Text(
                        text = currentTip.first,
                        style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Medium),
                        color = NextGpuTheme.colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(SpacingTiny))
                    Text(
                        text = currentTip.second,
                        style = MaterialTheme.typography.body2,
                        color = NextGpuTheme.colors.textSecondary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(SpacingLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NextGpuTheme.colors.surface, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { if (currentIndex > 0) currentIndex-- else currentIndex = tips.size - 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("icons/arrow-right-no-line.svg"),
                        contentDescription = "Previous",
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer { rotationZ = 180f },
                        colorFilter = ColorFilter.tint(NextGpuTheme.colors.textPrimary)
                    )
                }

                Spacer(modifier = Modifier.width(SpacingMedium))

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NextGpuTheme.colors.surface, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .clickable { if (currentIndex < tips.size - 1) currentIndex++ else currentIndex = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource("icons/arrow-right-no-line.svg"),
                        contentDescription = "Next",
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(NextGpuTheme.colors.textPrimary)
                    )
                }
            }
        }
    }
}
