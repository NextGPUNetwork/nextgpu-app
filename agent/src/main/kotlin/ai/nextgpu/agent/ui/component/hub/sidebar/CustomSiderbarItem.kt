package ai.nextgpu.agent.ui.component.hub.sidebar

// Compose UI and Foundation
import ai.nextgpu.agent.ui.theme.ElevationNone
import ai.nextgpu.agent.ui.theme.HeightButtonCompact
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Assuming these are your internal project resources/constants
// You may need to adjust these paths based on your actual project structure
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusSmall
import ai.nextgpu.agent.ui.theme.SpacingMicro
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.agent.ui.theme.HeightListItem
import ai.nextgpu.agent.ui.theme.IconSizeSidebar
import ai.nextgpu.agent.ui.theme.RadiusRound
import androidx.compose.material.Surface

@Composable
fun CustomSidebarItem(
    iconPath: String,
    label: String,
    isCollapsed: Boolean,
    onClick: () -> Unit,
    iconTint: Color = Color.Unspecified, // Default to no tint (native colors)
    hoverBackgroundColor: Color = NextGpuTheme.colors.hoverBackground,
    defaultTextColor: Color = NextGpuTheme.colors.textPrimary,
    hoverTextColor: Color = NextGpuTheme.colors.textPrimary,
    rippleColor: Color = Color.Unspecified,
    trailingContent: @Composable (RowScope.() -> Unit)? = null // Optional trailing icons/badges
) {
    val hoverShape = RoundedCornerShape(RadiusRound)
    val startPadding = 9.dp

    // Manual hover tracking for custom coloring
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Surface(
        color = Color.Transparent,
        shape = hoverShape,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightButtonCompact + 5.dp)
            .padding(vertical = SpacingMicro)
            .clip(hoverShape)
            .hoverable(interaction)
            .background(
                color = if (hovered) hoverBackgroundColor else Color.Transparent,
                shape = hoverShape
            )
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true, color = rippleColor),
                onClick = onClick
            ),
        elevation = ElevationNone
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.Start
        ) {
            if (!isCollapsed) {
                Spacer(modifier = Modifier.width(startPadding))
            }

            Icon(
                painter = painterResource(iconPath),
                contentDescription = if (isCollapsed) label else null,
                modifier = Modifier.size(IconSizeSidebar),
                tint = iconTint
            )

            if (!isCollapsed) {
                Spacer(modifier = Modifier.Companion.width(SpacingSmall + 5.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.subtitle2,
                    textAlign = TextAlign.Start,
                    color = if (hovered) hoverTextColor else defaultTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Render any trailing icons/components if provided
                if (trailingContent != null) {
                    trailingContent()
                    Spacer(modifier = Modifier.width(SpacingSmall))
                }
            }
        }
    }
}