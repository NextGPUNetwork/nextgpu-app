package ai.nextgpu.agent.ui.component.provider

import ai.nextgpu.agent.ui.component.provider.model.ProviderViewModel
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun ProviderScreen(
    viewModel: ProviderViewModel,
    isSidebarCollapsed: Boolean, // Passed from App.kt hoisted state
    onToggleSidebar: () -> Unit,
    onReturn: () -> Unit // Routes back to Hub
) {
    // Local state to track which sub-page of the provider screen is active
    var currentTab by remember { mutableStateOf("dashboard") }

    Surface(color = NextGpuTheme.colors.backgroundVariant) {
        Row(modifier = Modifier.fillMaxSize()) {

            // Provider Sidebar
            ProviderSidebar(
                isCollapsed = isSidebarCollapsed,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onSettings = { /* TODO: Open Settings Popup */ },
                onHelp = { /* TODO: Open Help link */ },
                onSwitchToHub = onReturn
            )

            // 2. Main Content Area
            Box(modifier = Modifier.weight(1f)) {
                MainProviderContent(
                    viewModel = viewModel,
                    currentTab = currentTab
                )
            }
        }
    }
}


@Composable
fun MainProviderContent(
    viewModel: ProviderViewModel,
    currentTab: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Clip the top-left corner to create the distinct content box shape
            .clip(RoundedCornerShape(topStart = RadiusLarge))
            // Apply the lighter/distinct background color
            .background(NextGpuTheme.colors.background)
            .padding(SpacingExtraLarge)
    ) {
        // Simple routing engine for the provider pages
        when (currentTab) {
            "dashboard" -> {
                Dashboard(
                    viewModel = viewModel
                )
            }
            "machine" -> {
                Text("My Machine Details", style = MaterialTheme.typography.h4, color = NextGpuTheme.colors.textPrimary)
            }
            "sessions" -> {
                Text("Session History", style = MaterialTheme.typography.h4, color = NextGpuTheme.colors.textPrimary)
            }
            "earnings" -> {
                Text("Earnings Report", style = MaterialTheme.typography.h4, color = NextGpuTheme.colors.textPrimary)
            }
        }
    }
}