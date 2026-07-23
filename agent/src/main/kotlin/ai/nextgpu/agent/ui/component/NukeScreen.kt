package ai.nextgpu.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.res.painterResource
import kotlin.system.exitProcess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NukeScreen(
    onProceed: () -> Unit,
    onReturn: () -> Unit,
) {
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }
    val scope = rememberCoroutineScope()

    var textInput by remember { mutableStateOf("") }
    var isNuking by remember { mutableStateOf(false) }
    var nukingError by remember { mutableStateOf<String?>(null) }
    val isNukeEnabled = textInput == "nuke" && !isNuking

    // Use a semi-transparent background to give the feel of a modal/popup
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary03Black.copy(alpha = 0.8f)), // Slight alpha for better overlay effect
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(RadiusMedium),
            color = NextGpuTheme.colors.surface,
            contentColor = NextGpuTheme.colors.textPrimary,
            modifier = Modifier
                .widthIn(min = 400.dp, max = 550.dp)
                .padding(horizontal = SpacingLarge)
        ) {
            Column(
                modifier = Modifier.padding(SpacingDialog), // Matched padding from standard popup
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = SpacingSmall)
                ) {
                    Icon(
                        painter = painterResource("icons/warning.svg"), // Ensure you have this SVG in your resources
                        contentDescription = "Warning",
                        tint = NextGpuTheme.colors.error,
                        modifier = Modifier.size(IconSizeMedium) // Adjust size to perfectly align with your h6 text
                    )
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "Danger Zone",
                        style = NextGpuTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = NextGpuTheme.colors.error
                    )
                }

                Divider(
                    color = NextGpuTheme.colors.border,
                    thickness = BorderWidth,
                    modifier = Modifier.padding(vertical = SpacingMedium)
                )

                // Warning Text
                Text(
                    text = "Nuking the application will permanently erase:",
                    style = NextGpuTheme.typography.body2,
                    color = NextGpuTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(SpacingSmall))

                Column(modifier = Modifier.padding(start = SpacingMedium)) {
                    Text("1. Chat history", style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                    Text("2. Models downloaded", style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                    Text("3. Hardware/Benchmark reports", style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                    Text("4. Usage statistics", style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                    Text("5. Security keys", style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                }

                Spacer(modifier = Modifier.height(SpacingMedium))

                Text(
                    text = "After this action, no recovery is possible.",
                    style = NextGpuTheme.typography.body2,
                    color = NextGpuTheme.colors.error,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(SpacingMedium))

                Text(
                    text = "Are you sure you want to do this? If so, please type in \"nuke\" (without quotes) in the following text box.",
                    style = NextGpuTheme.typography.caption,
                    color = NextGpuTheme.colors.textSecondary
                )

                Spacer(modifier = Modifier.height(SpacingMedium))

                // Input Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = {
                        Text("Type 'nuke'", color = NextGpuTheme.colors.textSecondary)
                    },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = NextGpuTheme.colors.error,
                        unfocusedBorderColor = NextGpuTheme.colors.border,
                        cursorColor = NextGpuTheme.colors.error,
                        textColor = NextGpuTheme.colors.textPrimary,
                        backgroundColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(RadiusMedium)
                )

                nukingError?.let {
                    Spacer(modifier = Modifier.height(SpacingSmall))
                    Text(
                        text = it,
                        style = NextGpuTheme.typography.caption,
                        color = NextGpuTheme.colors.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(SpacingLarge))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomButton(
                        text = "Cancel",
                        onClick = onReturn,
                        enabled = !isNuking,
                        backgroundColor = Color.Transparent,
                        textColor = NextGpuTheme.colors.textSecondary,
                        elevation = false
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    CustomButton(
                        text = if (isNuking) "Nuking..." else "Nuke",
                        enabled = isNukeEnabled,
                        onClick = {
                            scope.launch {
                                isNuking = true
                                nukingError = null
                                try {
                                    val success = withContext(Dispatchers.IO) {
                                        service.nukeInstance()
                                    }
                                    if (success) {
                                        exitProcess(0)
                                    } else {
                                        nukingError = "Nuke operation did not complete successfully."
                                        isNuking = false
                                    }
                                } catch (e: Exception) {
                                    nukingError = e.message ?: "Nuke operation failed."
                                    isNuking = false
                                }
                            }
                        },
                        backgroundColor = NextGpuTheme.colors.error,
                        textColor = Color.White,
                        disabledBackgroundColor = NextGpuTheme.colors.error.copy(alpha = 0.3f),
                        disabledTextColor = Color.White.copy(alpha = 0.5f),
                        elevation = false
                    )
                }
            }
        }
    }
}