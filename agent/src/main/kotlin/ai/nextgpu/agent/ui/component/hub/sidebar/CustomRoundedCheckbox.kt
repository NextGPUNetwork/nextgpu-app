package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*

/**
 * A custom implementation of a checkbox to support specific border styling and animations
 * that are difficult to achieve with the standard Material Checkbox.
 */
@Composable
fun CustomRoundedCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true // Added enabled state for cursor and click logic
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) Primary02Purple else Color.Transparent,
        label = "boxColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Primary02Purple else NextGpuTheme.colors.border,
        label = "borderColor"
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor,
        border = if (!checked) BorderStroke(BorderWidth, borderColor) else null,
        modifier = modifier
            .size(IconSizeSmall)
            // Sets the cursor to a pointing hand when enabled, and default arrow when disabled
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled, // Pass enabled state to clickable
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource("icons/check.svg"),
                    contentDescription = null,
                    tint = NextGpuTheme.colors.background,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}