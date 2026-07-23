package ai.nextgpu.agent.ui

import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.theme.BorderWidth
import ai.nextgpu.agent.ui.theme.HeightButtonCompact
import ai.nextgpu.agent.ui.theme.HeightTopBar
import ai.nextgpu.agent.ui.theme.IconSizeLarge
import ai.nextgpu.agent.ui.theme.IconSizeMedium
import ai.nextgpu.agent.ui.theme.IconSizeMicro
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.Primary01White
import ai.nextgpu.agent.ui.theme.RadiusRound
import ai.nextgpu.agent.ui.theme.RadiusSmall
import ai.nextgpu.agent.ui.theme.SidebarWidth
import ai.nextgpu.agent.ui.theme.SpacingLarge
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingTiny
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple // if using compose 1.7+, otherwise use rememberRipple()
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

// ==========================================
// CUSTOM HEADER COMPONENTS
// ==========================================
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.Surface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NextGpuTopBar(
    isSidebarVisible: Boolean,
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    showReadyToServe: Boolean,
    showUpdateBadge: Boolean,
    onUpdateClick: () -> Unit,
    onProfileClick: () -> Unit,
    windowState: WindowState,
    onClose: () -> Unit,
    isPrivateMode: Boolean,
    onPrivateModeChange: (Boolean) -> Unit,
    isProviderView: Boolean,
) {
    // TopBar Profile States
    val profileInteraction = remember { MutableInteractionSource() }
    val isProfileHovered by profileInteraction.collectIsHoveredAsState()
    var isProfileActive by remember { mutableStateOf(false) }

    // Sidebar Toggle States
    val sidebarInteraction = remember { MutableInteractionSource() }
    val isSidebarHovered by sidebarInteraction.collectIsHoveredAsState()

    // Smoothly animate the background split when the sidebar collapses!
    val currentSidebarWidth by animateDpAsState(targetValue = if (isSidebarCollapsed) 80.dp else 260.dp)

    Row(
        modifier = Modifier.fillMaxWidth().height(HeightTopBar),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- LEFT SIDE (Sidebar Header Area) ---
        if (isSidebarVisible) {
            Box(
                modifier = Modifier
                    .width(currentSidebarWidth) // <--- Uses the animated width
                    .fillMaxHeight()
                    .background(NextGpuTheme.colors.backgroundVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.padding(start = SpacingLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Optional: You could swap this to a smaller N logo when collapsed if you want!
                    Image(
                        painter = painterResource("images/nextgpu-primary-logo.svg"),
                        contentDescription = "Logo",
                        modifier = Modifier.height(IconSizeLarge)
                    )

                    Spacer(modifier = Modifier.width(SpacingMedium))

                    // THE SIDEBAR TOGGLE BUTTON
                    Box(
                        modifier = Modifier
                            .size(IconSizeMedium) // Small, subtle circle matching your mockup
                            .clip(CircleShape)
                            .hoverable(sidebarInteraction)
                            .background(
                                color = NextGpuTheme.colors.hoverBackground,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = sidebarInteraction,
                                indication = ripple(bounded = true),
                                onClick = onToggleSidebar
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource("icons/arrow-right-no-line.svg"), // Replace with your arrow SVG
                            contentDescription = "Toggle Sidebar",
                            tint = NextGpuTheme.colors.textSecondary,
                            modifier = Modifier
                                .size(10.dp)
                                // Flips the arrow mathematically when collapsed!
                                .graphicsLayer(rotationZ = if (isSidebarCollapsed) 180f else 0f)
                        )
                    }
                }
            }
        }

        // Right side (Main App Header Area)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(NextGpuTheme.colors.backgroundVariant) // Matches main content background
                .padding(end = SpacingMedium),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showUpdateBadge) {
                CustomButton(
                    text = "Update Available",
                    onClick = onUpdateClick,
                    modifier = Modifier
                        .padding(end = SpacingMedium)
                        .height(HeightButtonCompact), // Keeps it neatly sized within the 48dp top bar
                    shape = RoundedCornerShape(RadiusRound), // Perfect pill shape
                    backgroundColor = NextGpuTheme.colors.primary,
                    textColor = NextGpuTheme.colors.textPrimary,
                    elevation = false, // Keep it flat against the header
                    textStyle = MaterialTheme.typography.body2, // Smaller, crisp text for a badge
                    contentPadding = PaddingValues(horizontal = SpacingMedium)
                )
            }

            // Dynamic Buttons
//            if (showReadyToServe) {
//                Surface(
//                    shape = CircleShape,
//                    color = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f),
//                    modifier = Modifier.padding(end = SpacingMedium)
//                ) {
//                    Text(
//                        text = "Ready to Serve",
//                        color = NextGpuTheme.colors.primaryVariant,
//                        style = MaterialTheme.typography.body2,
//                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                    )
//                }
//            }

            val privateInteractionSource = remember { MutableInteractionSource() }
            val isPrivateHovered by privateInteractionSource.collectIsHoveredAsState()
            if (!isProviderView) {
                TooltipArea(
                    tooltip = {
                        // This is the actual UI of the tooltip bubble
                        Surface(
                            shape = RoundedCornerShape(RadiusSmall),
                            color = NextGpuTheme.colors.surface,
                            elevation = 4.dp,
                            border = BorderStroke(BorderWidth, NextGpuTheme.colors.border)
                        ) {
                            Text(
                                text = if (isPrivateMode) "Disable Private Mode" else "Enable Private Mode",
                                style = MaterialTheme.typography.caption,
                                color = NextGpuTheme.colors.textPrimary,
                                modifier = Modifier.padding(horizontal = SpacingMedium, vertical = SpacingTiny)
                            )
                        }
                    },
                    delayMillis = 500, // Standard 0.5s delay before showing

                    // CHANGED: Use ComponentRect to anchor to the button's layout bounds
                    tooltipPlacement = TooltipPlacement.ComponentRect(
                        anchor = Alignment.BottomCenter,   // Anchor to the bottom center of the Box
                        alignment = Alignment.TopCenter,   // Align the top center of the tooltip to that anchor
                        offset = DpOffset(0.dp, 24.dp)      // Adds a nice 8dp gap between the button and the tooltip
                    )
                ) {
                    // Your original Box goes perfectly inside here
                    Box(
                        modifier = Modifier
                            .size(IconSizeLarge)
                            .clip(CircleShape)
                            .hoverable(privateInteractionSource)
                            .background(
                                color = if (isPrivateMode) NextGpuTheme.colors.textSecondary
                                else if (isPrivateHovered) NextGpuTheme.colors.hoverBackground
                                else NextGpuTheme.colors.backgroundVariant,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = privateInteractionSource,
                                indication = ripple(bounded = true)
                            ) { onPrivateModeChange(!isPrivateMode) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource("icons/incognito-profile.svg"),
                            contentDescription = "Toggle Private Mode",
                            modifier = Modifier.size(IconSizeMedium),
                            tint = if (isPrivateMode) NextGpuTheme.colors.background else NextGpuTheme.colors.textSecondary,
                        )
                    }
                }
            }



            // --- UPDATED PROFILE ICON ---
//            Box(
//                modifier = Modifier
//                    .size(IconSizeLarge) // Adjusted slightly to provide a nice background bounding box
//                    .clip(CircleShape)
//                    .hoverable(profileInteraction)
//                    .background(
//                        color = if (isProfileActive) NextGpuTheme.colors.textSecondary
//                        else if (isProfileHovered) NextGpuTheme.colors.hoverBackground // Fallback if hoverBackground isn't defined
//                        else Color.Transparent,
//                        shape = CircleShape
//                    )
//                    .clickable(
//                        interactionSource = profileInteraction,
//                        indication = ripple(bounded = true), // or rememberRipple() depending on your Compose version
//                        onClick = {
//                            isProfileActive = !isProfileActive // Toggle the visual state
//                            onProfileClick()
//                        }
//                    ),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    painter = painterResource("icons/user-outlined.svg"),
//                    contentDescription = "Profile",
//                    tint = if (isProfileActive) NextGpuTheme.colors.background
//                    else NextGpuTheme.colors.primaryVariant,
//                    modifier = Modifier.size(IconSizeMedium) // Inner icon size
//                )
//            }

            Spacer(modifier = Modifier.width(SpacingLarge))

            // Window Controls
            NextGpuWindowControls(windowState, onClose)
        }
    }
}