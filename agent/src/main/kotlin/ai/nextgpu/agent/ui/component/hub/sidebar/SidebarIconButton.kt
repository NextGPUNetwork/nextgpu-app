package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import ai.nextgpu.agent.ui.theme.*

/**
 * Reusable Icon Button wrapper that adds a consistent Rounded Hover background.
 */
@Composable
fun SidebarIconButton(
    icon: String,
    onClick: () -> Unit,
    tint: Color,
    enabled: Boolean = true,
    size: Dp = IconSizeStandard, // Default 24dp
    iconSize: Dp = IconSizeSidebar // Default 18dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(RadiusSmall))
            .hoverable(interactionSource)
            .background(
                color = if (isHovered && enabled) NextGpuTheme.colors.hoverBackground else Color.Transparent
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource, // This handles the hover state automatically
                indication = ripple(bounded = true),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource("icons/$icon.svg"),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}
