package ai.nextgpu.agent.ui.component.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.foundation.hoverable
import kotlin.math.max
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PromptMode { TEXT, IMAGE }

@Composable
fun PromptRegion(
    sessionId: String?,
    promptText: TextFieldValue,
    onPromptChange: (TextFieldValue) -> Unit,
    onSendPrompt: () -> Unit,
    isGenerating: Boolean,
    onCancel: () -> Unit,
    currentMode: PromptMode,
    onModeChange: (PromptMode) -> Unit,
    installedComfyModels: List<String>,
    onGenerateImage: (pos: String, neg: String, width: Int, height: Int) -> Unit,
    // Opens Settings popup directly on the Models tab
    onOpenModelSettings: () -> Unit,
    audioRecorderService: ai.nextgpu.agent.service.AudioRecorderService,
    onCheckSttHealth: suspend () -> Boolean,
    onTranscribeAudio: suspend (java.io.File) -> String?
) {

    // =========================================================
    // Image generation state
    // =========================================================
    var negativePrompt by remember { mutableStateOf(TextFieldValue("")) }
    var selectedSize by remember { mutableStateOf("1024x1024") }


    // Derived: drives which UI branch renders in IMAGE mode
    val hasInstalledComfyModel = installedComfyModels.isNotEmpty()

    // =========================================================
    // Hysteresis for text field expansion (prevents layout loop)
    // =========================================================
    var isMultiLine by remember { mutableStateOf(false) }
    var expandTriggerLength by remember { mutableStateOf(Int.MAX_VALUE) }

    val focusRequester = remember { FocusRequester() }

    var isRecording by remember(sessionId) { mutableStateOf(false) }
    var isSttProcessing by remember(sessionId) { mutableStateOf(false) }
    val audioAmplitudes = remember(sessionId) {
        mutableStateListOf<Float>().apply { addAll(List(55) { 0.1f }) }
    }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(sessionId) {
        onDispose {
            if (isRecording) {
                // Silently stop recording and trash the temp file
                try { audioRecorderService.stopRecording().delete() } catch (e: Exception) {}
            }
        }
    }

    LaunchedEffect(isMultiLine, isRecording, isSttProcessing) {
        // Only attempt to grab focus if we are NOT in the recording overlay
        if (!isRecording && !isSttProcessing) {
            // Yield for a few milliseconds to ensure MainTextField is fully attached to the UI tree
            delay(50)
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Safely swallow the error if the composition is still transitioning
            }
        }
    }

    LaunchedEffect(promptText.text) {
        if (promptText.text.isEmpty()) {
            isMultiLine = false
            expandTriggerLength = Int.MAX_VALUE
        } else if (promptText.text.contains('\n')) {
            isMultiLine = true
            expandTriggerLength = 0
        } else if (isMultiLine && promptText.text.length < expandTriggerLength) {
            isMultiLine = false
            expandTriggerLength = Int.MAX_VALUE
        }
    }


    val startRecording = {
        coroutineScope.launch(Dispatchers.IO) {
            val isHealthy = onCheckSttHealth()
            if (isHealthy) {
                // Ensure we update state on Main thread
                withContext(Dispatchers.Main) {
                    isRecording = true
                }
                try {
                    audioRecorderService.startRecording { amplitude ->
                        val boosted = (kotlin.math.sqrt(amplitude) * 5.0f).coerceIn(0.2f, 1.0f)
                        val smoothed = (audioAmplitudes.last() * 0.3f) + (boosted * 0.7f)

                        // Protect against index out of bounds during rapid resets
                        if (audioAmplitudes.isNotEmpty()) {
                            audioAmplitudes.removeAt(0)
                            audioAmplitudes.add(smoothed)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isRecording = false
                    }
                }
            }
        }
    }

    val stopAndProcessRecording = { sendImmediately: Boolean ->
        // Remember which chat we were in when we clicked stop
        val capturedSessionId = sessionId

        isRecording = false
        isSttProcessing = true
        audioAmplitudes.clear()
        audioAmplitudes.addAll(List(55) { 0.1f })

        coroutineScope.launch(Dispatchers.IO) {
            val audioFile = audioRecorderService.stopRecording()

            try {
                val transcribedText = onTranscribeAudio(audioFile)

                withContext(Dispatchers.Main) {
                    // 4. CONTEXT CHECK: Only apply text if the user hasn't switched chats!
                    if (sessionId == capturedSessionId && !transcribedText.isNullOrBlank()) {
                        if (sendImmediately) {
                            onPromptChange(TextFieldValue(transcribedText))
                            delay(50) // Give Compose a frame to update the UI state
                            onSendPrompt()
                        }else {
                            val currentText = promptText.text
                            val space = if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " " else ""
                            onPromptChange(TextFieldValue(currentText + space + transcribedText))
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                withContext(Dispatchers.Main) {
                    // Only drop the loading spinner if we are still in the original chat
                    if (sessionId == capturedSessionId) {
                        isSttProcessing = false
                    }
                }
                audioFile.delete() // Clean up temp file!
            }
        }
    }

    val cornerRadius by animateDpAsState(
        targetValue = if (currentMode == PromptMode.IMAGE || isMultiLine) RadiusExtraLarge else RadiusRound
    )

    // Send is disabled in IMAGE mode when no ComfyUI model is installed
    val isSendEnabled = promptText.text.isNotBlank() &&
            (currentMode == PromptMode.TEXT || hasInstalledComfyModel)

    val handleEnterPress: () -> Unit = {
        if (isRecording) {
            stopAndProcessRecording(true)
        } else if (isSendEnabled) {
            if (currentMode == PromptMode.TEXT) {
                onSendPrompt()
            } else {
                val dimensions = selectedSize.split("x")
                val width = dimensions.getOrNull(0)?.toIntOrNull() ?: 1024
                val height = dimensions.getOrNull(1)?.toIntOrNull() ?: 1024
                onGenerateImage(promptText.text, negativePrompt.text, width, height)
            }
        }
    }

    // =========================================================
    // Main text input — extracted to avoid duplication across layouts
    // =========================================================
    @Composable
    fun MainTextField(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier.heightIn(min = HeightInputMin),
            contentAlignment = Alignment.CenterStart
        ) {
            if (promptText.text.isEmpty()) {
                Text(
                    text = if (currentMode == PromptMode.TEXT) "Feel free to ask me anything"
                    else "Describe what you want to generate",
                    style = MaterialTheme.typography.body1,
                    color = NextGpuTheme.colors.textSecondary
                )
            }
            BasicTextField(
                value = promptText,
                onValueChange = onPromptChange,
                enabled = !isGenerating,
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = HeightPrompt)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            if (event.isShiftPressed) {
                                val text = promptText.text
                                val selection = promptText.selection
                                val newText = text.substring(0, selection.start) + "\n" + text.substring(selection.end)
                                onPromptChange(TextFieldValue(newText, TextRange(selection.start + 1)))
                                return@onPreviewKeyEvent true
                            } else {
                                handleEnterPress()
                                return@onPreviewKeyEvent true
                            }
                        }
                        false
                    },
                onTextLayout = { textLayoutResult ->
                    if (!isMultiLine && textLayoutResult.lineCount > 1) {
                        isMultiLine = true
                        expandTriggerLength = max(0, promptText.text.length - 15)
                    }
                },
                textStyle = MaterialTheme.typography.body1.copy(color = NextGpuTheme.colors.textPrimary),
                cursorBrush = SolidColor(NextGpuTheme.colors.textPrimary)
            )
        }
    }

    // =========================================================
    // Image mode controls — shared between single-line and multi-line
    // layouts so the logic lives in exactly one place.
    //
    // hasInstalledComfyModel = true  → dimension + model dropdowns
    // hasInstalledComfyModel = false → "Download a model" CTA button
    // =========================================================
    @Composable
    fun ImageModeControls() {
        if (hasInstalledComfyModel) {
            Spacer(modifier = Modifier.width(SpacingMedium))
            ImageConfigDropdown(
                value = selectedSize,
                options = listOf("1024x1024", "1024x576", "576x1024", "512x512"),
                onSelect = { selectedSize = it }
            )
            Spacer(modifier = Modifier.width(SpacingSmall))

        } else {
            // No ComfyUI model installed — prompt the user to download one
            Spacer(modifier = Modifier.width(SpacingMedium))
            Box(
                modifier = Modifier
                    .height(HeightButtonCompact)
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(NextGpuTheme.colors.primaryVariant.copy(alpha = 0.12f))
                    .clickable(onClick = onOpenModelSettings)
                    .padding(horizontal = SpacingLarge),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource("icons/download.svg"),
                        contentDescription = null,
                        tint = NextGpuTheme.colors.primaryVariant,
                        modifier = Modifier.size(IconSizeMicro)
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "Download a model",
                        style = MaterialTheme.typography.body2,
                        fontWeight = FontWeight.Medium,
                        color = NextGpuTheme.colors.primaryVariant
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingLarge)
            .padding(bottom = SpacingMedium),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(cornerRadius)),
            shape = RoundedCornerShape(cornerRadius),
            color = NextGpuTheme.colors.background,
            elevation = ElevationNone
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SpacingLarge)
            ) {
                if (isRecording || isSttProcessing) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(HeightInputMin), // Match normal input height
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        PromptIconButton(
                            onClick = { stopAndProcessRecording(false) },
                            enabled = !isSttProcessing,
                            iconPath = "icons/stop.svg",
                            contentDescription = "Stop & Edit",
                            hoverBackgroundColor = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.2f),
                            backgroundColor = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f),
                            iconTint = NextGpuTheme.colors.primaryVariant
                        )

                        // Dynamic Visualizer
                        WaveformVisualizer(
                            amplitudes = audioAmplitudes,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = SpacingExtraLarge * 4)
                        )


                        PromptActionIconButton(
                            isGenerating = false, // Ensure this is false during STT
                            isLoading = isSttProcessing, // Triggers the new spinner
                            isEnabled = !isSttProcessing,
                            onSend = { stopAndProcessRecording(true) },
                            onCancel = { /* Cancel in-flight STT if you wire that up later */ }
                        )
                    }
                } else if (isMultiLine) {
                    // ---- MULTI-LINE LAYOUT (stacked) ----
                    MainTextField(modifier = Modifier.fillMaxWidth().padding(bottom = SpacingMedium))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PromptModeSelector(
                            currentMode = currentMode,
                            onModeChange = { newMode -> onModeChange(newMode) },
                            enabled = !isGenerating
                        )

                        if (currentMode == PromptMode.IMAGE) {
                            ImageModeControls()
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        PromptActionIconButton(
                            isGenerating = isGenerating,
                            isEnabled = isSendEnabled,
                            onSend = handleEnterPress,
                            onCancel = onCancel
                        )
                    }
                } else {
                    // ---- SINGLE-LINE LAYOUT (inline row) ----
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PromptModeSelector(
                            currentMode = currentMode,
                            onModeChange = { newMode -> onModeChange(newMode) },
                            enabled = !isGenerating
                        )

                        Spacer(modifier = Modifier.width(SpacingMedium))

                        MainTextField(modifier = Modifier.weight(1f))

                        if (currentMode == PromptMode.IMAGE) {
                            ImageModeControls()
                        }

                        Spacer(modifier = Modifier.width(SpacingMedium))


                        PromptIconButton(
                            onClick = { startRecording() },
                            iconPath = "icons/mic.svg",
                            contentDescription = "Voice Prompt",
                            hoverBackgroundColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.1f),
                            enabled = !isGenerating,
                            iconTint = NextGpuTheme.colors.textSecondary,
                            iconSize = IconSizeMedium
                        )

                        Spacer(modifier = Modifier.width(SpacingSmall))

                        PromptActionIconButton(
                            isGenerating = isGenerating,
                            isEnabled = isSendEnabled,
                            onSend = handleEnterPress,
                            onCancel = onCancel
                        )
                    }
                }

                // Negative prompt — only when IMAGE mode and a model is installed
                AnimatedVisibility(
                    visible = currentMode == PromptMode.IMAGE && hasInstalledComfyModel,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = SpacingMedium)
                            .clip(RoundedCornerShape(RadiusMedium))
                            .background(NextGpuTheme.colors.backgroundVariant)
                            .padding(horizontal = SpacingMedium, vertical = SpacingSmall),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (negativePrompt.text.isEmpty()) {
                            Text(
                                text = "Describe negative prompt here (optional) to avoid particular things such as grass, extra fingers, etc.",
                                style = MaterialTheme.typography.body2,
                                color = NextGpuTheme.colors.textSecondary
                            )
                        }
                        BasicTextField(
                            value = negativePrompt,
                            onValueChange = { negativePrompt = it },
                            enabled = !isGenerating,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.body2.copy(color = NextGpuTheme.colors.textPrimary),
                            cursorBrush = SolidColor(NextGpuTheme.colors.textPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptModeSelector(
    currentMode: PromptMode,
    onModeChange: (PromptMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusLarge))
                .background(NextGpuTheme.colors.backgroundVariant)
                .clickable(enabled = enabled) { expanded = true }
                .padding(horizontal = SpacingMedium, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentMode == PromptMode.TEXT) "Text chat" else "Generate image",
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.width(SpacingSmall))
            Icon(
                painter = painterResource("icons/arrow-down.svg"),
                contentDescription = null,
                tint = NextGpuTheme.colors.textSecondary,
                modifier = Modifier.size(12.dp).rotate(rotation)
            )
        }

        MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(NextGpuTheme.colors.background)
                    .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
            ) {
                DropdownMenuItem(onClick = { onModeChange(PromptMode.TEXT); expanded = false }) {
                    Text("Text chat", color = NextGpuTheme.colors.textPrimary)
                }
                DropdownMenuItem(onClick = { onModeChange(PromptMode.IMAGE); expanded = false }) {
                    Text("Generate image", color = NextGpuTheme.colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun ImageConfigDropdown(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(RadiusSmall))
                .clickable { expanded = true }
                .padding(horizontal = SpacingSmall, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource("icons/arrow-down.svg"),
                contentDescription = null,
                tint = NextGpuTheme.colors.textSecondary,
                modifier = Modifier.size(10.dp).rotate(rotation)
            )
        }

        MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(NextGpuTheme.colors.background)
                    .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(onClick = { onSelect(option); expanded = false }) {
                        Text(option, color = NextGpuTheme.colors.textPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun PromptActionIconButton(
    isGenerating: Boolean,
    isLoading: Boolean = false, // <-- New state
    isEnabled: Boolean,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    if (isLoading) {
        // --- NEW: Spinner State ---
        Box(
            modifier = modifier
                .size(SpacingHuge)
                .background(NextGpuTheme.colors.hoverBackground, CircleShape)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(IconSizeSmall),
                color = NextGpuTheme.colors.textSecondary,
                strokeWidth = 2.dp
            )
        }
    } else if (isGenerating) {
        // --- EXISTING: Cancel Generation State ---
        Box(
            modifier = modifier
                .size(SpacingHuge)
                .alpha(if (isHovered) 0.8f else 1f)
                .background(NextGpuTheme.colors.primaryVariant.copy(0.2f), CircleShape)
                .clip(CircleShape)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onCancel
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource("icons/stop.svg"),
                modifier = Modifier.size(IconSizeSmall),
                contentDescription = "Cancel",
                tint = NextGpuTheme.colors.primaryVariant,
            )
        }
    } else {
        // --- EXISTING: Send State ---
        Box(
            modifier = modifier
                .size(SpacingHuge)
                .alpha(if (isHovered && isEnabled) 0.8f else 1f)
                .background(
                    if (isEnabled) NextGpuTheme.colors.primary else NextGpuTheme.colors.hoverBackground,
                    CircleShape
                )
                .clip(CircleShape)
                .hoverable(interactionSource)
                .clickable(
                    enabled = isEnabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSend
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource("icons/arrow-up-line.svg"),
                contentDescription = "Send",
                tint = if (isEnabled) Color.Black else NextGpuTheme.colors.textSecondary,
                modifier = Modifier.size(IconSizeSmall)
            )
        }
    }
}