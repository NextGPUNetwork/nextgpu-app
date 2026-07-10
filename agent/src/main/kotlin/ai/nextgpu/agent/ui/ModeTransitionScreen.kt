package ai.nextgpu.agent.ui

import ai.nextgpu.agent.ui.theme.IconSizeExtraLarge
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.SpacingMedium
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun ModeTransitionScreen() {
    // Draws a full-screen solid primary (lime) background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGpuTheme.colors.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Using Icon instead of Image allows us to easily tint the SVG to black
            Icon(
                painter = painterResource("images/nextgpu-primary-logo.svg"), // Ensure this is your standalone 'N' logo
                contentDescription = "NextGPU Logo",
                tint = Color.Unspecified,
                modifier = Modifier.size(IconSizeExtraLarge * 2)
            )

            Spacer(modifier = Modifier.height(SpacingMedium))

            Text(
                text = "Switching user mode...",
                style = MaterialTheme.typography.body1,
                color = NextGpuTheme.colors.textPrimary,
            )
        }
    }
}