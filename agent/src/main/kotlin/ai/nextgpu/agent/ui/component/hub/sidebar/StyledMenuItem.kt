package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import ai.nextgpu.agent.ui.theme.*

/**
 * Standardized dropdown item with consistent padding, hover effects, and tinting logic.
 */
@Composable
fun StyledMenuItem(
    icon: String,
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    tint: Color? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Priority: Explicit Tint > Destructive Tint > Default Tint
    val iconTint = when {
        tint != null -> tint
        isDestructive -> ErrorText
        else -> NextGpuTheme.colors.textSecondary
    }

    DropdownMenuItem(
        onClick = onClick,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = RadiusMedium),
        modifier = Modifier.Companion
            .padding(vertical = SpacingMicro)
            .height(HeightButtonCompact)
            .clip(RoundedCornerShape(RadiusSmall))
            .background(if (isHovered) NextGpuTheme.colors.hoverBackground else Color.Companion.Transparent)
    ) {
        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
            Icon(
                painter = painterResource("icons/$icon.svg"),
                contentDescription = null,
                modifier = Modifier.Companion.size(IconSizeSmall),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.Companion.width(SpacingSmall))
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                color = if (isDestructive) ErrorText else NextGpuTheme.colors.textPrimary
            )
        }
    }
}
