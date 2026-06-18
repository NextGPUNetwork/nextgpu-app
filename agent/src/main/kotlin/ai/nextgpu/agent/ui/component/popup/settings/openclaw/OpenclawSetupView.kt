package ai.nextgpu.agent.ui.component.popup.settings.openclaw

import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ai.nextgpu.agent.util.OSUtil
import ai.nextgpu.agent.ui.theme.*

@Composable
fun OpenclawSetupView(onSetupComplete: () -> Unit) {
    // Read the state from the JSON file immediately
    var installState by remember { mutableStateOf(OSUtil.getOpenclawInstallState()) }

    // CRITICAL FIX: If the file says it's running, automatically resume the polling loop!
    var isInstalling by remember { mutableStateOf(installState.status == "RUNNING") }

    // CRITICAL FIX: Pre-load the logs so the terminal doesn't blink empty on reopen
    var rawLogs by remember { mutableStateOf(OSUtil.getOpenclawLogs()) }

    val terminalScrollState = rememberScrollState()
    val isInstallCompleted = installState.status == "COMPLETED" || installState.progressPercentage == 100
    val isFailed = installState.status == "FAILED"
    val hasStarted = installState.progressPercentage > 0

    val stepDescriptions = remember {
        mapOf(
            "init" to "Initializing OpenClaw Setup...",
            "prereqs" to "Installing OS prerequisites...",
            "nvm_setup" to "Configuring Node Version Manager...",
            "openclaw_install" to "Downloading and installing OpenClaw CLI...",
            "openclaw_config" to "Configuring Gateway and models...",
            "finished" to "OpenClaw configuration complete."
        )
    }

    var expandedStep by remember { mutableStateOf<Int?>(1) }

    LaunchedEffect(isInstalling) {
        if (isInstalling) {
            rawLogs = OSUtil.getOpenclawLogs()
            if (isInstalling) {
                rawLogs = OSUtil.getOpenclawLogs()
                while (isInstalling) {
                    val newState = OSUtil.getOpenclawInstallState()
                    rawLogs = OSUtil.getOpenclawLogs()
                    installState = newState

                    // Just stop polling when done or failed. Do NOT handle navigation here.
                    if (newState.status == "COMPLETED" || newState.progressPercentage == 100 || newState.status == "FAILED") {
                        isInstalling = false
                    }
                    delay(500)
                }
            }
        }
    }

    LaunchedEffect(rawLogs) {
        terminalScrollState.animateScrollTo(terminalScrollState.maxValue)
    }

    val setupBackgroundColor = if (!NextGpuTheme.colors.isDark) Secondary05LightPurple else NextGpuTheme.colors.background

    Row(modifier = Modifier.fillMaxSize()) {
        // --- LEFT COLUMN ---
        BoxWithConstraints(modifier = Modifier.weight(0.8f).fillMaxHeight().clipToBounds()) {
            val mascotWidth = maxWidth * 1.2f
            Image(
                painter = painterResource("images/openclaw-mascot.svg"),
                contentDescription = "OpenClaw illustration",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .requiredWidth(mascotWidth)
                    .graphicsLayer {
                        translationX = -size.width * 0.15f
                        translationY = size.height * 0.20f
                    },
                contentScale = ContentScale.FillWidth
            )

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = SpacingHuge, start = SpacingLarge, end = SpacingLarge),
                horizontalAlignment = Alignment.Start
            ) {
                Text("OpenClaw Integration", style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold), color = NextGpuTheme.colors.textPrimary, modifier = Modifier.padding(bottom = SpacingSmall))
                Text("Unlock advanced agent capabilities. We will automatically install the OpenClaw service, bind it to your local models, and configure the gateway.", style = MaterialTheme.typography.subtitle1, color = NextGpuTheme.colors.textSecondary, modifier = Modifier.width(420.dp))
            }
        }

        // --- RIGHT COLUMN ---
        Box(modifier = Modifier.weight(1.2f).fillMaxHeight().padding(vertical = SpacingLarge, horizontal = SpacingLarge)) {
            Column(
                modifier = Modifier.fillMaxSize().background(NextGpuTheme.colors.background, RoundedCornerShape(RadiusLarge)).border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusLarge)).padding(SpacingLarge)
            ) {
                Text(if (!hasStarted && !isInstallCompleted) "Installation overview" else "Installation progress", style = MaterialTheme.typography.h5, color = NextGpuTheme.colors.textPrimary, modifier = Modifier.padding(bottom = SpacingTiny))

                val progressText = if (isInstallCompleted) "100%" else "${installState.progressPercentage}%"
                Text(progressText, style = MaterialTheme.typography.h3.copy(fontWeight = FontWeight.Bold), color = NextGpuTheme.colors.textPrimary, modifier = Modifier.padding(bottom = SpacingMedium))

                val animatedProgress by animateFloatAsState(
                    targetValue = if (isInstallCompleted) 1f else installState.progressPercentage / 100f,
                    animationSpec = tween(durationMillis = 500, easing = LinearEasing)
                )

                Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(NextGpuTheme.colors.border.copy(alpha = 0.5f), RoundedCornerShape(50)).clip(RoundedCornerShape(50))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedProgress).fillMaxHeight().background(if (isFailed) NextGpuTheme.colors.error else OpenclawRed))
                }
                Spacer(modifier = Modifier.height(SpacingLarge))

                val isOverview = !hasStarted && !isInstallCompleted

                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = if (isOverview) Arrangement.spacedBy(SpacingMedium) else Arrangement.Top
                ) {
                    ExpandableStepCard(
                        stepNumber = 1,
                        title = "Service Configuration",
                        description = "Installs dependencies, configures the OpenClaw CLI, and launches the local gateway.",
                        isActive = isInstalling || hasStarted,
                        isDone = isInstallCompleted,
                        isExpanded = expandedStep == 1,
                        isOverview = isOverview,
                        iconPath = "icons/setup.svg",
                        onToggleExpand = { expandedStep = if (expandedStep == 1) null else 1 }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(300.dp).background(NextGpuTheme.colors.surface.copy(alpha = 0.8f), RoundedCornerShape(RadiusLarge)).padding(SpacingLarge)
                        ) {
                            val currentStepText = if (installState.currentStepName.isNotBlank()) "> ${stepDescriptions[installState.currentStepName] ?: installState.currentStepName}" else if (isInstallCompleted) "> Setup complete" else "> Preparing..."
                            Text(currentStepText, style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Normal), color = NextGpuTheme.colors.textPrimary)
                            Spacer(modifier = Modifier.height(SpacingMedium))

                            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(terminalScrollState)) {
                                Text(
                                    text = buildAnnotatedString {
                                        append(rawLogs.ifBlank { "Waiting for process to start..." })
                                        if (isFailed && !installState.error.isNullOrBlank()) {
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
                }

                Spacer(modifier = Modifier.height(SpacingMedium))

                val buttonText = when {
                    isInstalling -> "Installing..."
                    isFailed -> "Retry Installation"
                    isInstallCompleted -> "Continue ->"
                    hasStarted -> "Resume Installation"
                    else -> "Start Installation ->"
                }

                CustomButton(
                    text = buttonText,
                    enabled = !isInstalling, // Enabled when finished so they can click Continue!
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = {
                        if (isInstallCompleted) {
                            // The user clicked Continue. Fire the callback to update the DB and swap screens.
                            onSetupComplete()
                        } else {
                            // Start or Resume the installation
                            isInstalling = true
                            try {
                                OSUtil.startOpenclawInstallAsync()
                            } catch (e: Exception) {
                                val failedState = OSUtil.InstallState()
                                failedState.error = e.message
                                failedState.status = "FAILED"
                                installState = failedState
                                isInstalling = false
                            }
                        }
                    },
                    backgroundColor = OpenclawRed,
                    textColor = Primary01White,
                    disabledBackgroundColor = OpenclawRed.copy(alpha = 0.5f)
                )
            }
        }
    }
}