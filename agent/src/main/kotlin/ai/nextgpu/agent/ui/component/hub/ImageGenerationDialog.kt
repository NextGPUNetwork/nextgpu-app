package ai.nextgpu.agent.ui.component.hub

import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import ai.nextgpu.agent.ui.component.NumericStepper
import ai.nextgpu.agent.ui.theme.*

@Composable
fun ImageGenerationDialog(
    showDialog: Boolean,
    modelTemplates: List<String>,
    onDismissRequest: () -> Unit,
    onGenerate: (String, String, Int, Int, String) -> Unit
) {
    if (!showDialog) return

    var positivePrompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var imageWidth by remember { mutableStateOf(512) }
    var imageHeight by remember { mutableStateOf(512) }
    var imageModelTemplate by remember { mutableStateOf("") }

    // Update selection if 'default' is not in modelTemplates but we have other options
    LaunchedEffect(modelTemplates) {
        if (imageModelTemplate !in modelTemplates && modelTemplates.isNotEmpty()) {
            imageModelTemplate = modelTemplates.first()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(RadiusMedium),
            color = NextGpuTheme.colors.surface, // Adapts to Dark/Light mode safely
            contentColor = NextGpuTheme.colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge)
        ) {
            Column(
                modifier = Modifier.padding(
                    top = SpacingLarge,
                    start = SpacingLarge,
                    end = SpacingLarge,
                    bottom = SpacingMedium // Tighter bottom padding to balance button height
                )
            ) {
                // TITLE
                Text(
                    text = "Create Image",
                    style = NextGpuTheme.typography.h6,
                    color = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SpacingMedium)
                )

                // INPUT CONTENT (Scrollable to prevent keyboard overlap)
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false) // Allows it to shrink if keyboard appears
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    // POSITIVE PROMPT
                    OutlinedTextField(
                        value = positivePrompt,
                        onValueChange = { positivePrompt = it },
                        label = { Text("Positive prompt") },
                        placeholder = { Text("Describe what you want to visualize (e.g. sun setting behind K-2 mountain)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = NextGpuTheme.colors.textPrimary,
                            focusedLabelColor = NextGpuTheme.colors.textPrimary,
                            cursorColor = NextGpuTheme.colors.textPrimary,
                            unfocusedBorderColor = NextGpuTheme.colors.border,
                            unfocusedLabelColor = NextGpuTheme.colors.textSecondary,
                            placeholderColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.6f)
                        )
                    )

                    // NEGATIVE PROMPT
                    OutlinedTextField(
                        value = negativePrompt,
                        onValueChange = { negativePrompt = it },
                        label = { Text("Negative prompt") },
                        placeholder = { Text("Describe what to exclude (e.g. fog, grass)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = NextGpuTheme.colors.textPrimary,
                            focusedLabelColor = NextGpuTheme.colors.textPrimary,
                            cursorColor = NextGpuTheme.colors.textPrimary,
                            unfocusedBorderColor = NextGpuTheme.colors.border,
                            unfocusedLabelColor = NextGpuTheme.colors.textSecondary,
                            placeholderColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.6f)
                        )
                    )

                    // DIMENSION STEPPERS
                    CompositionLocalProvider(LocalContentColor provides NextGpuTheme.colors.textPrimary) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(SpacingLarge),
                            modifier = Modifier.padding(top = SpacingSmall)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                NumericStepper(
                                    label = "Width",
                                    value = imageWidth,
                                    onChange = { imageWidth = it },
                                    min = 128,
                                    max = 1024
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                NumericStepper(
                                    label = "Height",
                                    value = imageHeight,
                                    onChange = { imageHeight = it },
                                    min = 128,
                                    max = 1024
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(SpacingSmall))

                    // MODEL TEMPLATE SELECTOR
                    Text(
                        text = "Model template",
                        style = MaterialTheme.typography.body2,
                        color = NextGpuTheme.colors.textSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(SpacingMicro)) {
                        modelTemplates.forEach { model ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { imageModelTemplate = model }
                                    .padding(vertical = SpacingMicro),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (model == imageModelTemplate),
                                    onClick = { imageModelTemplate = model },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = NextGpuTheme.colors.primary,
                                        unselectedColor = NextGpuTheme.colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(SpacingSmall))
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.body1,
                                    color = NextGpuTheme.colors.textPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SpacingLarge))

                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CANCEL BUTTON
                    CustomButton(
                        text = "Cancel",
                        onClick = onDismissRequest,
                        hoverBackgroundColor = NextGpuTheme.colors.background.copy(0.35f),
                        backgroundColor = Color.Transparent,
                        textColor = NextGpuTheme.colors.textSecondary,
                        elevation = false,

                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    // GENERATE BUTTON
                    CustomButton(
                        text = "Generate",
                        // Optional: Disable button if the prompt is entirely empty to prevent bad generation calls
                        enabled = positivePrompt.isNotBlank() && imageModelTemplate.isNotBlank(),
                        onClick = {
                            onDismissRequest()
                            onGenerate(
                                positivePrompt,
                                negativePrompt,
                                imageWidth,
                                imageHeight,
                                imageModelTemplate
                            )
                        },
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,

                    )
                }
            }
        }
    }
}
