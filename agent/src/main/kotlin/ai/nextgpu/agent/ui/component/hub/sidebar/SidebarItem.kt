package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.unit.dp

@Composable
fun SidebarItem(
    icon: String,
    label: String,
    isCollapsed: Boolean,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val hoverShape = RoundedCornerShape(RadiusSmall)
    val startPadding = 8.dp

    Surface(
        color = if (isActive) NextGpuTheme.colors.hoverBackground else Color.Transparent,
        shape = hoverShape,
        modifier = Modifier.Companion
            .fillMaxWidth()
            .height(HeightListItem)
            .padding(vertical = SpacingMicro)
            .clip(hoverShape)
            .let { base ->
                // Manual hover/click handling to ensure specific background styling
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                base
                    .hoverable(interaction)
                    .background(
                        color = if (hovered && !isActive) NextGpuTheme.colors.hoverBackground else Color.Companion.Transparent,
                        shape = hoverShape
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = ripple(bounded = true),
                        onClick = onClick ?: {}
                    )
            },
        elevation = ElevationNone
    ) {
        Row(
            modifier = Modifier.Companion.fillMaxSize(),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {

            Spacer(modifier = Modifier.Companion.width(startPadding))


            Icon(
                painter = painterResource("icons/$icon.svg"),
                contentDescription = if (isCollapsed) label else null,
                modifier = Modifier.Companion.size(IconSizeSidebar),
                tint = NextGpuTheme.colors.textPrimary
            )

            if (!isCollapsed) {
                Spacer(modifier = Modifier.Companion.width(SpacingSmall))
                Text(
                    text = label,
                    style = MaterialTheme.typography.subtitle2,
                    textAlign = TextAlign.Companion.Start,
                    color = NextGpuTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                    modifier = Modifier.Companion.weight(1f)
                )
            }
        }
    }
}
