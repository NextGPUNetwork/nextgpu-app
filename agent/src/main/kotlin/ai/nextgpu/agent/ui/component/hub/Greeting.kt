package ai.nextgpu.agent.ui.component.hub

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*

@Composable
fun Greeting() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Main Greeting with Icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource("images/stars.svg"), // Assuming you have a stars/sparkles icon
                contentDescription = null,
                tint = NextGpuTheme.colors.primaryVariant,
                // THEME: Replaced 32.dp with IconSizeLarge
                modifier = Modifier.size(IconSizeLarge).padding(end = SpacingSmall)
            )
            Text(
                text = "Let's get started!",
                style = MaterialTheme.typography.h4.copy(fontWeight = FontWeight.SemiBold),
                color = NextGpuTheme.colors.textPrimary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(SpacingSmall))

        // Optional Subtitle - simplified as requested
        Text(
            text = "What would you like to build or explore today?",
            color =  NextGpuTheme.colors.textSecondary,
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
    }
}
