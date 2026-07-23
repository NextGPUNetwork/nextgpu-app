package ai.nextgpu.agent.ui.component.popup.settings.openclaw

import ai.nextgpu.agent.ui.component.CustomButton
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import ai.nextgpu.agent.ui.component.popup.settings.components.SettingsItemRow
import ai.nextgpu.agent.ui.component.popup.settings.components.SettingsToggle
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

import androidx.compose.ui.window.Dialog

@Composable
fun OpenclawSettingsView(viewModel: SettingsViewModel) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    // UI states for loading indicators
    var isUninstalling by remember { mutableStateOf(false) }
    var isOpeningPortal by remember { mutableStateOf(false) }

    // NEW: Dialog state
    var showUninstallDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = SpacingLarge, vertical = SpacingMedium)
    ) {
        // Dashboard Action
        SettingsButtonRow(
            title = "Open Dashboard",
            description = "Access the OpenClaw web interface to manage skills, memory, and runtime settings.",
            buttonText = if (isOpeningPortal) "Opening..." else "Open Portal",
            onClick = {
                if (!isOpeningPortal) {
                    isOpeningPortal = true
                    scope.launch {
                        // Dynamically fetch the URL from WSL
                        val url = viewModel.fetchOpenclawDashboardUrl()
                        // Open the URL
                        uriHandler.openUri(url)
                        // Reset the button state
                        isOpeningPortal = false
                    }
                }
            }
        )

        Divider(color = NextGpuTheme.colors.border, modifier = Modifier.padding(vertical = SpacingMedium))

        // Navigation Shortcut Toggle
        SettingsToggle(
            title = "Pin to Navigation Bar",
            description = "Add a quick-access button for the OpenClaw portal directly to the main app sidebar.",
            isChecked = viewModel.showOpenclawShortcut,
            onCheckedChange = { viewModel.toggleOpenclawShortcut(it) }
        )

        Divider(color = NextGpuTheme.colors.border, modifier = Modifier.padding(vertical = SpacingMedium))

        // Uninstall Action
        SettingsButtonRow(
            title = "Uninstall Integration",
            description = "Remove the OpenClaw service, gateway daemon, and local configuration files from your system.",
            buttonText = if (isUninstalling) "Uninstalling..." else "Uninstall",
            isDestructive = true,
            onClick = {
                if (!isUninstalling) {
                    showUninstallDialog = true // Trigger dialog instead of immediate uninstall
                }
            }
        )
    }

    // --- Uninstall Confirmation Dialog ---
    if (showUninstallDialog) {
        Dialog(onDismissRequest = { showUninstallDialog = false }) {
            Surface(
                shape = RoundedCornerShape(RadiusMedium),
                color = NextGpuTheme.colors.surface,
                contentColor = NextGpuTheme.colors.textPrimary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
            ) {
                Column(
                    modifier = Modifier.padding(
                        top = SpacingDialog,
                        start = SpacingDialog,
                        end = SpacingDialog,
                        bottom = SpacingLarge // Tighter bottom padding to balance button height
                    )
                ) {
                    Text(
                        text = "Uninstall OpenClaw?",
                        style = NextGpuTheme.typography.h6,
                        modifier = Modifier.padding(bottom = SpacingSmall)
                    )

                    Text(
                        text = "Are you sure you want to completely remove the OpenClaw service and its configuration? You will need to run the setup process again to use advanced agent capabilities.",
                        style = NextGpuTheme.typography.body2,
                        color = NextGpuTheme.colors.textSecondary
                    )

                    Spacer(modifier = Modifier.height(SpacingLarge))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomButton(
                            text = "Cancel",
                            onClick = { showUninstallDialog = false },
                            backgroundColor = Color.Transparent,
                            textColor = NextGpuTheme.colors.textSecondary,
                            elevation = false,
                            contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                        )

                        Spacer(modifier = Modifier.width(SpacingSmall))

                        CustomButton(
                            text = "Uninstall",
                            onClick = {
                                showUninstallDialog = false
                                isUninstalling = true
                                viewModel.uninstallOpenclaw()
                                viewModel.toggleOpenclawShortcut(false)
                            },
                            backgroundColor = NextGpuTheme.colors.error,
                            textColor = Primary01White,
                            elevation = false,
                            contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsButtonRow(
    title: String,
    description: String,
    buttonText: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    SettingsItemRow(title = title, description = description) {
        CustomButton(
            text = buttonText,
            onClick = onClick,
            backgroundColor = if (isDestructive) NextGpuTheme.colors.error.copy(alpha = 0.1f) else NextGpuTheme.colors.primaryVariant.copy(alpha = 0.15f),
            textColor = if (isDestructive) NextGpuTheme.colors.error else NextGpuTheme.colors.primaryVariant,
            elevation = false,
            contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
        )
    }
}