package ai.nextgpu.agent.ui.component.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.Text
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.provider.model.ProviderViewModel
import ai.nextgpu.agent.ui.component.provider.model.ProviderSetupState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun Dashboard(viewModel: ProviderViewModel) {

    LaunchedEffect(Unit) {
        if (viewModel.uiState == ProviderSetupState.CHECKING_HARDWARE) {
            viewModel.performColdHardwareCheck()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (viewModel.uiState) {
            ProviderSetupState.CHECKING_HARDWARE -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NextGpuTheme.colors.primaryVariant)
                    if (viewModel.statusMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(SpacingMedium))
                        Text(text = viewModel.statusMessage, style = NextGpuTheme.typography.body2, color = NextGpuTheme.colors.textSecondary)
                    }
                }
            }
            ProviderSetupState.INCOMPATIBLE_HARDWARE -> {
                IncompatibleHardwareView(onRetry = viewModel::performColdHardwareCheck)
            }
            ProviderSetupState.AWAITING_OTP -> {
                ConnectionFormView(
                    securityCode = viewModel.securityCode,
                    errorMessage = viewModel.statusMessage,
                    onCodeChange = viewModel::updateSecurityCode,
                    onConnect = viewModel::submitOtp
                )
            }
            ProviderSetupState.READY_TO_APPLY -> {
                ApplyAsProviderView(
                    onApply = viewModel::executeRegistrationPipeline,
                    errorMessage = viewModel.statusMessage,
                    )
            }
//            ProviderSetupState.PROCESSING_APPLICATION -> {
//                Column(horizontalAlignment = Alignment.CenterHorizontally) {
//                    CircularProgressIndicator(color = NextGpuTheme.colors.primaryVariant)
//                    Spacer(modifier = Modifier.height(SpacingMedium))
//                    Text(text = viewModel.statusMessage, style = NextGpuTheme.typography.body1, color = NextGpuTheme.colors.textPrimary)
//                }
//            }
            ProviderSetupState.PROCESSING_APPLICATION -> {
                AuditProgressView(
                    statusMessage = viewModel.statusMessage,
                    progress = viewModel.auditProgress
                )
            }
            ProviderSetupState.ACTIVE_PROVIDER -> {
                SuccessView()
            }
            ProviderSetupState.APPLICATION_FAILED -> {
                ApplicationFailedView(
                    errorMessage = viewModel.statusMessage,
                    onRetry = viewModel::executeRegistrationPipeline
                )
            }

            ProviderSetupState.ACTIVE_PROVIDER -> {
                SuccessView()
            }
        }
    }
}

@Composable
fun ConnectionFormView(
    securityCode: String,
    errorMessage: String,
    onCodeChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 490.dp)
    ) {
        Text("Connect to NextGPU", style = NextGpuTheme.typography.h4, color = NextGpuTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(SpacingLarge))

        val annotatedInstructions = buildAnnotatedString {
            append("Copy the temporary security code from your browser dashboard at \n")

            // Start annotation for the clickable link
            pushStringAnnotation(tag = "URL", annotation = "https://provider.nextgpu.ai")
            withStyle(
                style = SpanStyle(
                    color = NextGpuTheme.colors.primaryVariant,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("provider.nextgpu.ai")
            }
            pop() // End annotation

            append(" and paste it below.")
        }
        ClickableText(
            text = annotatedInstructions,
            style = NextGpuTheme.typography.body1.copy(
                color = NextGpuTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            ),
            onClick = { offset ->
                // Check if the clicked character has a "URL" annotation
                annotatedInstructions.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        // Open the link in the browser
                        uriHandler.openUri(annotation.item)
                    }
            }
        )
        Spacer(modifier = Modifier.height(SpacingExtraLarge))
        OutlinedTextField(
            value = securityCode,
            onValueChange = onCodeChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(RadiusMedium),
            isError = errorMessage.isNotBlank(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = NextGpuTheme.colors.textPrimary,
                focusedBorderColor = NextGpuTheme.colors.primaryVariant,
                unfocusedBorderColor = NextGpuTheme.colors.border,
                errorBorderColor = MaterialTheme.colors.error,
                cursorColor = NextGpuTheme.colors.textPrimary,
                errorCursorColor = MaterialTheme.colors.error
            )
        )
        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(SpacingMedium))
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error,
                style = NextGpuTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(SpacingExtraLarge))

        CustomButton(
            text = "Connect",
            onClick = onConnect,
            backgroundColor = NextGpuTheme.colors.primary,
            textColor = Color.Black
        )
    }
}

@Composable
fun IncompatibleHardwareView(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Incompatible Hardware", style = NextGpuTheme.typography.h4, color = NextGpuTheme.colors.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("NextGPU requires a dedicated NVIDIA GPU to operate.", color = NextGpuTheme.colors.textSecondary)
        Spacer(modifier = Modifier.height(16.dp))
        CustomButton(text = "Re-scan Hardware", onClick = onRetry)
    }
}

@Composable
fun ApplyAsProviderView(
    errorMessage: String, // <-- New parameter to catch the server crash messages
    onApply: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 490.dp)
    ) {
        Text(
            text = "Become a Provider",
            style = NextGpuTheme.typography.h4,
            color = NextGpuTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        val annotatedInstructions = buildAnnotatedString {
            append("To get started, we will detect your hardware, benchmark your system, and conduct a secure audit.")
        }

        Text(
            text = annotatedInstructions,
            style = NextGpuTheme.typography.body1,
            color = NextGpuTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )

        // --- NEW ERROR BLOCK START ---
        if (errorMessage.isNotBlank()) {
            Spacer(modifier = Modifier.height(SpacingLarge))
            Text(
                text = errorMessage,
                color = MaterialTheme.colors.error, // Matches the styling from ConnectionFormView
                style = NextGpuTheme.typography.body2,
                textAlign = TextAlign.Center
            )
        }
        // --- NEW ERROR BLOCK END ---

        Spacer(modifier = Modifier.height(SpacingExtraLarge))

        CustomButton(
            text = if (errorMessage.isNotBlank()) "Retry Application" else "Apply as a Provider", // Subtle UX improvement
            onClick = onApply,
            backgroundColor = NextGpuTheme.colors.primary,
            textColor = Color.Black
        )
    }
}

@Composable
fun SuccessView() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Onboarding Successful!",
            style = NextGpuTheme.typography.h4,
            color = NextGpuTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(SpacingMedium))
        Text(
            text = "You are now registered as a provider! Soon, you'll be able to share your GPU power and start earning.",
            style = NextGpuTheme.typography.body1,
            color = NextGpuTheme.colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ApplicationFailedView(errorMessage: String, onRetry: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 500.dp)
    ) {
        Text(
            text = "Hardware Audit Failed",
            style = NextGpuTheme.typography.h4,
            color = NextGpuTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(SpacingLarge))

        // Determine the error message first
        val defaultError = "Hardware audit completed, but your system does not meet the minimum requirements."
        val actualError = errorMessage.ifBlank { defaultError }

        // Combine the error message AND the support links into one single continuous paragraph
        val combinedText = buildAnnotatedString {
            append(actualError)
            append(" ") // Single space instead of a line break

            append("If you believe this is a mistake, contact us at ")

            // Email Link
            pushStringAnnotation(tag = "URI", annotation = "mailto:info@nextgpu.ai")
            withStyle(
                style = SpanStyle(
                    color = NextGpuTheme.colors.primaryVariant,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("info@nextgpu.ai")
            }
            pop()

            append(" or reach out to us on ")

            // Telegram Link
            pushStringAnnotation(tag = "URI", annotation = "https://t.me/JoinNextGPU")
            withStyle(
                style = SpanStyle(
                    color = NextGpuTheme.colors.primaryVariant,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Telegram")
            }
            pop()

            append(" or ")

            // Discord Link
            pushStringAnnotation(tag = "URI", annotation = "https://discord.com/invite/PPnHMEwwU4")
            withStyle(
                style = SpanStyle(
                    color = NextGpuTheme.colors.primaryVariant,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("Discord")
            }
            pop()

            append(".")
        }

        ClickableText(
            text = combinedText,
            style = NextGpuTheme.typography.body1.copy(
                color = NextGpuTheme.colors.textSecondary,
                textAlign = TextAlign.Center
            ),
            onClick = { offset ->
                combinedText.getStringAnnotations(tag = "URI", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            uriHandler.openUri(annotation.item)
                        } catch (e: Exception) {
                            // Failsafe in case a desktop environment lacks a default mail client or browser
                        }
                    }
            }
        )

        Spacer(modifier = Modifier.height(SpacingExtraLarge))

        CustomButton(
            text = "Retry Audit",
            onClick = onRetry,
            backgroundColor = NextGpuTheme.colors.primary,
            textColor = Color.Black
        )
    }
}

@Composable
fun AuditProgressView(
    statusMessage: String,
    progress: Float
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
        label = "AuditProgressAnimation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.widthIn(max = 490.dp)
    ) {
        // Centered, smaller, bold heading
        Text(
            text = "Hardware Audit in Progress",
            style = NextGpuTheme.typography.h5.copy(fontWeight = FontWeight.Bold),
            color = NextGpuTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = SpacingExtraLarge)
        )

        // Status Message (Left) and Percentage (Right) on top of the progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp), // Small gap between the text and the bar
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = statusMessage,
                style = NextGpuTheme.typography.body1,
                color = NextGpuTheme.colors.textPrimary
            )

            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = NextGpuTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                color = NextGpuTheme.colors.textPrimary
            )
        }

        // Linear Progress Bar
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
    }
}