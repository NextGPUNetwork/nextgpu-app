package ai.nextgpu.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.hub.sidebar.CustomRoundedCheckbox

/**
 * WelcomeScreen is the initial screen shown to users after login.
 * It provides information about the NextGPU Agent and its functionality.
 */
@Composable
fun WelcomeScreen(
    onProceed: () -> Unit,
) {
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }

    // UI state for preferences and EULA acceptance
    var eulaAccepted by remember { mutableStateOf(false) }

    // Coroutine scope for launching tasks in the background
    val scope = rememberCoroutineScope()

    val uriHandler = LocalUriHandler.current
    val eulaUrl = remember {
        // Try to resolve the EULA from resources (classpath)
        object {}.javaClass.classLoader.getResource("eula.html")?.toExternalForm()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NextGpuTheme.colors.background)
            .padding(SpacingLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SpacingLarge),
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            // NextGPU full logo
            val logoResource = if (NextGpuTheme.colors.isDark) {
                "images/nextgpu-secondary-logo-full.svg" // Make sure this file exists
            } else {
                "images/nextgpu-primary-logo-full.svg"
            }
            Image(
                painter = painterResource(logoResource),
                contentDescription = "NextGPU Logo",
                modifier = Modifier.width(210.dp).padding(bottom = SpacingTiny),
            )

            val launchResource = if (NextGpuTheme.colors.isDark) {
                "images/launch-dark.svg" // Make sure this file exists
            } else {
                "images/launch.svg"
            }
            Image(
                painter = painterResource(launchResource),
                contentDescription = "Launch image",
                modifier = Modifier.size(192.dp)
            )
            Text(
                text = "Welcome to NextGPU",
                style = MaterialTheme.typography.h4,
                color = NextGpuTheme.colors.textPrimary,
            )
            Text(
                text = "Let's get started. We will inspect your hardware to see how much power you can harness with your computer.",
                style = MaterialTheme.typography.subtitle2,
                color = NextGpuTheme.colors.textSecondary,
            )
            // EULA acceptance row
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CustomRoundedCheckbox(
                    checked = eulaAccepted,
                    onCheckedChange = { eulaAccepted = it }
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "I accept the ", color = NextGpuTheme.colors.textPrimary,  style = MaterialTheme.typography.subtitle2,)
                    Text(
                        text = "End-User License Agreement",
                        style = MaterialTheme.typography.subtitle2,
                        color = NextGpuTheme.colors.primaryVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable(enabled = eulaUrl != null) {
                                eulaUrl?.let { uriHandler.openUri(it) }
                            }
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Get started Button
                CustomButton(
                    text = "Get Started",
                    onClick = {
                        val eulaProperty = service.getGlobalProperty(GlobalPropertyConfig.IS_EULA_ACCEPTED)
                        eulaProperty.valueReference = if (eulaAccepted) "true" else "false"
                        service.saveGlobalProperty(eulaProperty)
                        onProceed()
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    enabled = eulaAccepted,
                    // We match the styling of the previous button
                    backgroundColor = NextGpuTheme.colors.primary,
                    textColor = Primary03Black,
                    // Explicitly setting the disabled background to 50% alpha as per original code
                    disabledBackgroundColor = NextGpuTheme.colors.primary.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun PreferenceCheck(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = PrimaryText02,
                uncheckedColor = PrimaryText01,
                checkmarkColor = Accent01Lime
            )
        )
        Spacer(modifier = Modifier.width(SpacingSmall))
        Text(text = label, color = PrimaryText01, style = MaterialTheme.typography.body1)
    }
}
