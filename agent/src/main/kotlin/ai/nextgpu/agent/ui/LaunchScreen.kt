package ai.nextgpu.agent.ui

import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LaunchScreen(
    onProceed: () -> Unit
) {
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }
    val installProfile = remember {
        service.getGlobalProperty(GlobalPropertyConfig.INSTALL_PROFILE)?.valueReference ?: ""
    }

    val isProvider = installProfile == "provider"

    val subtitleText = if (isProvider) {
        "Your machine is configured and ready to join the network.\nClick below to connect your node and start earning."
    } else {
        "You can now run fully private AI models on your machine, locally and securely.\nThe era of sovereign AI begins with the button below."
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGpuTheme.colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            val logoResource = if (NextGpuTheme.colors.isDark) {
                "images/nextgpu-secondary-logo-full.svg"
            } else {
                "images/nextgpu-primary-logo-full.svg"
            }
            Image(
                painter = painterResource(logoResource),
                contentDescription = "NextGPU Logo",
                modifier = Modifier
                    .width(160.dp) // Adjust based on your SVG's native proportions
                    .padding(bottom = SpacingExtraLarge)
            )

            // Main Title
            Text(
                text = "Let’s launch NextGPU",
                style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Bold),
                color = NextGpuTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = SpacingMedium)
            )

            // Subtitle
            Text(
                text = subtitleText,
                style = MaterialTheme.typography.body1,
                color = NextGpuTheme.colors.textSecondary,
                textAlign = TextAlign.Center, // Centers the text lines
                modifier = Modifier
                    .fillMaxWidth() // Ensures it takes up the full width to center properly
                    .padding(bottom = SpacingLarge) // Large gap before the button
            )

            // Wide Launch Button
            CustomButton(
                text = "Launch NextGPU",
                onClick = onProceed,
                modifier = Modifier
                    .fillMaxWidth(0.5f) // Makes the button span half the screen width, matching the design
                    .height(56.dp),
                backgroundColor = NextGpuTheme.colors.primary,
                textColor = Primary03Black
            )
        }
    }
}