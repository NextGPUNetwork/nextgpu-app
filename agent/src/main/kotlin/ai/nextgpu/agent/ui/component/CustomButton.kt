package ai.nextgpu.agent.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.runtime.getValue

enum class IconPosition {
    Start, End
}

// Helper function to dynamically lighten a color by a certain fraction
private fun Color.lighten(fraction: Float = 0.5f): Color {
    return Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = alpha
    )
}

@Composable
fun CustomButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    iconPosition: IconPosition = IconPosition.End,
    shape: Shape = RoundedCornerShape(RadiusRound),
    backgroundColor: Color = NextGpuTheme.colors.primary,
    textColor: Color = NextGpuTheme.colors.textPrimary,
    // Nullable props: If null, it automatically lightens the base color
    hoverBackgroundColor: Color? = null,
    hoverTextColor: Color? = null,
    disabledBackgroundColor: Color = backgroundColor.copy(alpha = 0.5f),
    disabledTextColor: Color = textColor.copy(alpha = 0.5f),
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = SpacingExtraLarge, vertical = SpacingSmall),
    elevation: Boolean = true
) {
    // Track hover state manually
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // Smart defaults: Fall back to lightened version if no specific hover color is provided
    val targetHoverBg = hoverBackgroundColor ?: backgroundColor.lighten(0.15f)
    val targetHoverText = hoverTextColor ?: textColor

    // Smoothly animate the colors
    val currentBgColor by animateColorAsState(
        targetValue = when {
            !enabled -> disabledBackgroundColor
            isHovered -> targetHoverBg
            else -> backgroundColor
        },
        animationSpec = tween(durationMillis = 150) // Crisp, snappy fade
    )

    val currentTextColor by animateColorAsState(
        targetValue = when {
            !enabled -> disabledTextColor
            isHovered -> targetHoverText
            else -> textColor
        },
        animationSpec = tween(durationMillis = 150)
    )

    // Using Surface instead of Button to completely bypass Material's ripple engine
    Surface(
        modifier = modifier
            // This sets the cursor to a pointer hand when enabled, and default arrow when disabled
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // THIS kills the default glass/ripple effect
                enabled = enabled,
                onClick = onClick
            ),
        shape = shape,
        color = currentBgColor,
        contentColor = currentTextColor,
        border = if (borderColor != null) BorderStroke(BorderWidth, borderColor) else null,
        elevation = if (elevation && enabled) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null && iconPosition == IconPosition.Start) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeSmall),
                    tint = currentTextColor
                )
                Spacer(modifier = Modifier.width(SpacingSmall))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.body1,
                color = currentTextColor,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            if (icon != null && iconPosition == IconPosition.End) {
                Spacer(modifier = Modifier.width(SpacingSmall))
                Icon(
                    painter = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizeSmall),
                    tint = currentTextColor
                )
            }
        }
    }
}