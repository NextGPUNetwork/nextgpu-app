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
import androidx.compose.ui.text.font.FontWeight
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.unit.dp

/**
 * Standardized dropdown item with consistent padding, hover effects, and tinting logic.
 */
@Composable
fun StyledMenuItem(
    text: String,
    onClick: () -> Unit,
    icon: String? = null, // Made optional
    isDestructive: Boolean = false,
    tint: Color? = null,
    fontWeight: FontWeight = FontWeight.Normal // Added to support selection state
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
        modifier = Modifier
            .padding(vertical = SpacingMicro)
            .height(HeightButtonCompact)
            .clip(RoundedCornerShape(RadiusSmall))
            .background(if (isHovered) NextGpuTheme.colors.hoverBackground else Color.Transparent)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Only render icon and spacer if an icon string is provided
            if (icon != null) {
                Icon(
                    painter = painterResource("icons/$icon.svg"),
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeSmall),
                    tint = iconTint,
                )
                Spacer(modifier = Modifier.width(SpacingMedium + 3.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.body2,
                fontWeight = fontWeight, // Apply the custom weight
                color = if (isDestructive) ErrorText else NextGpuTheme.colors.textPrimary
            )
        }
    }
}