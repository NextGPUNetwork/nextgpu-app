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
import ai.nextgpu.agent.ui.theme.*
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
            .background(Primary03Black),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 400.dp, max = 550.dp)
                .padding(SpacingMedium),
            shape = RoundedCornerShape(RadiusMedium),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = ElevationLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(SpacingLarge)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⚠️", style = MaterialTheme.typography.h5)
                    Spacer(modifier = Modifier.width(SpacingSmall))
                    Text(
                        text = "DANGER ZONE",
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = ErrorText
                    )
                }

                Spacer(modifier = Modifier.height(SpacingMedium))
                Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(SpacingMedium))

                // Warning Text
                Text(
                    text = "Nuking the application will permanently erase:",
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(SpacingSmall))

                Column(modifier = Modifier.padding(start = SpacingMedium)) {
                    Text("1. Chat history", style = MaterialTheme.typography.body2)
                    Text("2. Models downloaded", style = MaterialTheme.typography.body2)
                    Text("3. Hardware/Benchmark reports", style = MaterialTheme.typography.body2)
                    Text("4. Usage statistics", style = MaterialTheme.typography.body2)
                    Text("5. Security keys", style = MaterialTheme.typography.body2)
                }

                Spacer(modifier = Modifier.height(SpacingMedium))

                Text(
                    text = "After this action, no recovery is possible.",
                    style = MaterialTheme.typography.body2,
                    color = ErrorText,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(SpacingMedium))

                Text(
                    text = "Are you sure you want to do this? If so, please type in \"nuke\" (without quotes) in the following text box",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(SpacingMedium))

                // Input Field
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Type 'nuke'") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = ErrorText,
                        cursorColor = ErrorText,
                        textColor = MaterialTheme.colors.onSurface
                    )
                )

                nukingError?.let {
                    Spacer(modifier = Modifier.height(SpacingSmall))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2,
                        color = ErrorText,
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
                    TextButton(
                        onClick = onReturn,
                        enabled = !isNuking
                    ) {
                        Text("Cancel", color = MaterialTheme.colors.onSurface)
                    }

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    Button(
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
                        enabled = isNukeEnabled,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = ErrorText,
                            contentColor = Color.White,
                            disabledBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                        ),
                        shape = RoundedCornerShape(RadiusSmall)
                    ) {
                        Text(if (isNuking) "NUKING..." else "NUKE")
                    }
                }
            }
        }
    }
}
