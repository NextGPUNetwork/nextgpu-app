package ai.nextgpu.agent.ui.component.hub.sidebar

import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.Dialog
import ai.nextgpu.agent.ui.theme.*

@Composable
fun RenameDialog(
    title: String = "Rename Chat Session",
    subtitle: String = "Enter a new name for this chat (max 30 characters):",
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    // Upgrade to TextFieldValue to control the cursor position.
    // Initialize the selection property to place the cursor at the end of the initial text.
    var nameValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(initialName.length)
            )
        )
    }

    // Convenience variable to keep the rest of the logic clean
    val nameText = nameValue.text

    // Create Focus Requesters
    val nameFocusRequester = remember { FocusRequester() }
    val cancelFocusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }

    // Request focus on the Text Field when dialog opens
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
                // Optional: Allow Esc key to dismiss the dialog
                .onKeyEvent { event ->
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
                    bottom = SpacingLarge // Balances bottom button height
                )
            ) {
                // TITLE
                Text(
                    text = title,
                    style = NextGpuTheme.typography.h6,
                    color = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.padding(bottom = SpacingSmall)
                )

                // SUBTITLE
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        color = NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.padding(bottom = SpacingMedium)
                    )
                }

                // TEXT FIELD
                OutlinedTextField(
                    value = nameValue,
                    onValueChange = { newValue ->
                        // Apply the max character limit on value change, but preserve cursor movement
                        if (newValue.text.length <= 30) {
                            nameValue = newValue
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = { if (nameText.isNotBlank()) onConfirm(nameText) }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingLarge)
                        .focusRequester(nameFocusRequester)
                        // Route Tab key to the Cancel button next
                        .focusProperties { next = cancelFocusRequester }
                        .onKeyEvent { event ->
                            // Submit on hardware Enter key
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && nameText.isNotBlank()) {
                                onConfirm(nameText)
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
                    )
                )

                // ACTION BUTTONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // CANCEL BUTTON
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

                    // SAVE BUTTON
                    CustomButton(
                        text = "Rename",
                        enabled = nameText.isNotBlank(),
                        onClick = { onConfirm(nameText) },
                        backgroundColor = NextGpuTheme.colors.primary,
                        textColor = Primary03Black,
                        modifier = Modifier
                            .focusRequester(confirmFocusRequester)
                            .onKeyEvent { event ->
                                if (event.key == Key.Enter && event.type == KeyEventType.KeyUp && nameText.isNotBlank()) {
                                    onConfirm(nameText)
                                    true
                                } else false
                            }
                    )
                }
            }
        }
    }
}