package ai.nextgpu.agent.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.component.common.CustomButton
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.text.style.TextAlign

@Composable
fun LaunchScreen(
    onProceed: () -> Unit
) {
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
            // 1. Logo
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

            // 2. Main Title
            Text(
                text = "Let’s launch NextGPU",
                style = MaterialTheme.typography.h2.copy(fontWeight = FontWeight.Bold),
                color = NextGpuTheme.colors.textPrimary,
                modifier = Modifier.padding(bottom = SpacingMedium)
            )

            // 3. Subtitle Placeholder
            Text(
                text = "You can now run fully private AI models on your machine, locally and securely.\n" +
                        "The era of sovereign AI begins with the button below.",
                style = MaterialTheme.typography.body1,
                color = NextGpuTheme.colors.textSecondary,
                textAlign = TextAlign.Center, // Centers the text lines
                modifier = Modifier
                    .fillMaxWidth() // Ensures it takes up the full width to center properly
                    .padding(bottom = SpacingLarge) // Large gap before the button
            )

            // 4. Wide Launch Button
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