package ai.nextgpu.agent.ui.component.popup.settings.tabs

import androidx.compose.animation.Crossfade

import androidx.compose.runtime.*

import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.component.popup.settings.openclaw.OpenclawSettingsView
import ai.nextgpu.agent.ui.component.popup.settings.openclaw.OpenclawSetupView
import ai.nextgpu.agent.ui.theme.*

// --------------------------------------------------------
// 1. THE ROUTER (This forces the instant UI updates)
// --------------------------------------------------------
@Composable
fun OpenclawTabView(viewModel: SettingsViewModel) {
    // Reactively crossfade based on the ViewModel's state
    Crossfade(targetState = viewModel.isOpenclawSetupComplete, label = "OpenClawRouter") { isComplete ->
        if (isComplete) {
            OpenclawSettingsView(viewModel)
        } else {
            OpenclawSetupView(
                onSetupComplete = {
                    // Instantly updates UI and persists to the database
                    viewModel.updateOpenclawSetupComplete(true)
                }
            )
        }
    }
}