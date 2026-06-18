package ai.nextgpu.agent.ui.component.hub.sidebar

import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ai.nextgpu.agent.ui.theme.*

/**
 * Common dialog for Create and Update Project.
 */
@Composable
fun ProjectDialog(
    title: String,
    confirmLabel: String,
    name: String,
    onNameChange: (String) -> Unit,
    instructions: String,
    onInstructionsChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // Create Focus Requesters for explicit navigation
    val nameFocusRequester = remember { FocusRequester() }
    val instructionsFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    // Request focus on the Name field as soon as the dialog opens
    LaunchedEffect(Unit) {
        nameFocusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(RadiusMedium),
            color = NextGpuTheme.colors.surface,
            contentColor = NextGpuTheme.colors.textPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SpacingLarge)
                .onKeyEvent { event ->
                    // Optional: Close dialog on Escape key
                    if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                        onDismiss()
                        true
                    } else false
                }
        ) {
            Column(
                modifier = Modifier.padding(
                    top = SpacingDialog,
                    start = SpacingDialog,
                    end = SpacingDialog,
                    bottom = SpacingLarge // Balances the bottom button height
                )
            ) {
                // TITLE
                Text(
                    text = title,
                    style = NextGpuTheme.typography.h6,
                    color = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SpacingMedium)
                )

                // PROJECT NAME FIELD
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 30) onNameChange(it) },
                    label = { Text("Project Name") },
                    placeholder = { Text("Short project Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { instructionsFocusRequester.requestFocus() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingSmall)
                        .focusRequester(nameFocusRequester)
                        .focusProperties { next = instructionsFocusRequester }
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                                // Jump to next field on Enter
                                instructionsFocusRequester.requestFocus()
                                true
                            } else false
                        },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NextGpuTheme.colors.textPrimary,
                        focusedLabelColor = NextGpuTheme.colors.textPrimary,
                        cursorColor = NextGpuTheme.colors.textPrimary,
                        unfocusedBorderColor = NextGpuTheme.colors.border,
                        unfocusedLabelColor = NextGpuTheme.colors.textSecondary,
                        placeholderColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                )

                // INSTRUCTIONS FIELD
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { if (it.length <= 4096) onInstructionsChange(it) },
                    label = { Text("Instructions") },
                    placeholder = { Text("System instructions for this project...") },
                    singleLine = false,
                    maxLines = 6,
                    // Default IME action allows the Enter key to create new lines
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .focusRequester(instructionsFocusRequester)
                        .focusProperties { next = cancelFocusRequester },
                    shape = RoundedCornerShape(RadiusMedium),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NextGpuTheme.colors.textPrimary,
                        focusedLabelColor = NextGpuTheme.colors.textPrimary,
                        cursorColor = NextGpuTheme.colors.textPrimary,
                        unfocusedBorderColor = NextGpuTheme.colors.border,
                        unfocusedLabelColor = NextGpuTheme.colors.textSecondary,
                        placeholderColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )
                )

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
                        onClick = onDismiss,
                        backgroundColor = Color.Transparent,
                        textColor = NextGpuTheme.colors.textSecondary,
                        hoverBackgroundColor = NextGpuTheme.colors.background.copy(0.35f),
                        elevation = false,
                        modifier = Modifier
                            .focusRequester(cancelFocusRequester)
                            .focusProperties { next = confirmFocusRequester }
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyUp) {
                                    onDismiss()
                                    true
                                } else false
                            }
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    // CONFIRM
                    CustomButton(
                        text = confirmLabel,
                        enabled = name.isNotBlank(),
                        onClick = onConfirm,
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,
                        modifier = Modifier
                            .focusRequester(confirmFocusRequester)
                            .onKeyEvent { event ->
                                // Allow Enter to submit if the button has focus and form is valid
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && name.isNotBlank()) {
                                    onConfirm()
                                    true
                                } else false
                            }
                    )
                }
            }
        }
    }
}