package ai.nextgpu.agent.ui.component.provider

import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.IconPosition
import ai.nextgpu.agent.ui.component.hub.sidebar.SidebarItem
import ai.nextgpu.agent.ui.theme.HeightListItem
import ai.nextgpu.agent.ui.theme.IconSizeSidebar
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusSmall
import ai.nextgpu.agent.ui.theme.SidebarCollapsedWidth
import ai.nextgpu.agent.ui.theme.SidebarWidth
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingMicro
import ai.nextgpu.agent.ui.theme.SpacingSmall
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun ProviderSidebar(
    isCollapsed: Boolean,
    currentTab: String,
    onTabSelected: (String) -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit,
    onSwitchToHub: () -> Unit
) {
    val sidebarWidth by animateDpAsState(targetValue = if (isCollapsed) SidebarCollapsedWidth else SidebarWidth)

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(NextGpuTheme.colors.backgroundVariant) // Seamlessly blends with TopBar
            .padding(SpacingMedium)
    ) {
        // --- TOP NAVIGATION TABS ---
        SidebarItem(
            icon = "home", // Assumes icons/dashboard.svg exists
            label = "Dashboard",
            isCollapsed = isCollapsed,
            isActive = currentTab == "dashboard",
            onClick = { onTabSelected("dashboard") }
        )
//        SidebarItem(
//            icon = "machine",
//            label = "My Machine",
//            isCollapsed = isCollapsed,
//            isActive = currentTab == "machine",
//            onClick = { onTabSelected("machine") }
//        )
//        SidebarItem(
//            icon = "sessions",
//            label = "Sessions",
//            isCollapsed = isCollapsed,
//            isActive = currentTab == "sessions",
//            onClick = { onTabSelected("sessions") }
//        )
//        SidebarItem(
//            icon = "earnings",
//            label = "Earnings",
//            isCollapsed = isCollapsed,
//            isActive = currentTab == "earnings",
//            onClick = { onTabSelected("earnings") }
//        )

        Spacer(modifier = Modifier.weight(1f)) // Pushes the rest to the bottom

        // --- BOTTOM ACTIONS ---
        SidebarItem(
            icon = "settings",
            label = "Settings",
            isCollapsed = isCollapsed,
            isActive = false,
            onClick = onSettings
        )
        SidebarItem(
            icon = "help",
            label = "Help",
            isCollapsed = isCollapsed,
            isActive = false,
            onClick = onHelp
        )

        // Toggle Pill Button (Matches mockup)
        CustomButton(
            text = if (isCollapsed) "" else "Switch to AI Hub",
            icon = painterResource("icons/switch.svg"),
            iconPosition = IconPosition.Start,
            onClick = onSwitchToHub,
            // Keep the exact same dimensions as SidebarItem at all times
            modifier = Modifier
                .fillMaxWidth()
                .height(HeightListItem)
                .padding(vertical = SpacingMicro),
            // Keep the exact same shape as SidebarItem at all times
            shape = CircleShape,
            backgroundColor = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f),
            textColor = NextGpuTheme.colors.primaryVariant,
            elevation = false,
            textStyle = MaterialTheme.typography.subtitle2,
            iconSize = IconSizeSidebar,
            iconOnlySize = IconSizeSidebar,
            contentPadding = if (isCollapsed) PaddingValues(0.dp) else PaddingValues(start = RadiusSmall, end = SpacingSmall)
        )
    }
}