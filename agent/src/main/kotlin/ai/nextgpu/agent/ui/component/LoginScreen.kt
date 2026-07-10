package ai.nextgpu.agent.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.theme.*
import kotlinx.coroutines.launch

/**
 * LoginScreen is the authentication screen for NextGPU Agent.
 * It allows users to authenticate using their wallet address and one-time key.
 */
@Composable
fun LoginScreen(
    onProceed: () -> Unit,
    onReturn: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var walletAddress by remember { mutableStateOf("") }
    var oneTimeKey by remember { mutableStateOf("") }

    MaterialTheme {
    Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

    // Main content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(SpacingLarge)
                    .zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingLarge)
            ) {
                // Header with logo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = SpacingSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource("images/nextgpu-primary-logo-full.svg"),
                        contentDescription = "NextGPU Logo",
                        modifier = Modifier.height(SpacingMassive)
                    )
                }

                // Login card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.5f),
                        elevation = 4.dp,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingExtraLarge),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(SpacingLarge)
                        ) {
                            // Card header
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(SpacingMedium),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Login to NextGPU",
                                    style = MaterialTheme.typography.h5,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Divider(
                                color = Primary01White,
                                thickness = 1.dp
                            )

                            // Wallet Address Input
                            OutlinedTextField(
                                value = walletAddress,
                                onValueChange = { walletAddress = it },
                                label = { Text("Wallet Address") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = TextFieldDefaults.outlinedTextFieldColors()
                            )

                            // One-time Key Input
                            OutlinedTextField(
                                value = oneTimeKey,
                                onValueChange = { oneTimeKey = it },
                                label = { Text("One-time Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Secondary03LightGray,
                                    focusedBorderColor = Secondary04DarkGray,
                                    unfocusedBorderColor = Secondary04DarkGray,
                                    backgroundColor = Primary01White
                                )
                            )

                            // Error message
                            errorMessage?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.error,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Login Button
                            Button(
                                onClick = {
                                    isLoading = true
                                    errorMessage = null

                                    scope.launch {
                                        try {
                                            val service = springContext.getBean(NextGpuAgentService::class.java)
                                            val success = service.authenticate(oneTimeKey)

                                            if (success) {
                                                onProceed()
                                            } else {
                                                errorMessage = "Authentication failed. Please check your credentials."
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = e.message ?: "An error occurred during authentication"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(SpacingMassive),
                                enabled = !isLoading && walletAddress.isNotBlank() && oneTimeKey.isNotBlank(),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(SpacingMedium),
                                        color = Color.Black
                                    )
                                } else {
                                    Text(
                                        text = "Login",
                                        color = Color.Black,
                                        style = MaterialTheme.typography.button,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(SpacingSmall))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForwardIos,
                                        contentDescription = "Login",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
