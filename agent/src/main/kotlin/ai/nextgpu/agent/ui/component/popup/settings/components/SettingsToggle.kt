package ai.nextgpu.agent.ui.component.popup.settings.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*

@Composable
fun SettingsToggle(
    title: String,
    description: String?,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsItemRow(title = title, description = description) {
        NextGpuSwitch(
            checked = isChecked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun NextGpuSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // track dimensions
    val width = 42.dp
    val height = 24.dp
    val thumbSize = 20.dp
    val gap = 2.dp

    // Color transition from border (off) to success (on)
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) NextGpuTheme.colors.primaryVariant else NextGpuTheme.colors.border,
        label = "SwitchColor"
    )

    // Smooth horizontal slide
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) width - thumbSize - gap else gap,
        label = "ThumbOffset"
    )

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(RadiusRound)) // THEME: 50.dp
            .background(backgroundColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        // The Thumb
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(thumbSize)
                .background(Color.White, CircleShape)
        )
    }
}
