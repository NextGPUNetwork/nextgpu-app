package ai.nextgpu.agent.ui.component.hub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.SpacingHuge
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

@Composable
fun WaveformVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = NextGpuTheme.colors.textSecondary,
    barWidth: Float = 4f,
    gain: Float = 2.0f // Boosts the volume significantly
) {
    val targetBarCount = 55

    // Map raw data to symmetric array
    val symmetricAmplitudes = remember(amplitudes.toList()) {
        val centerIndex = targetBarCount / 2
        List(targetBarCount) { index ->
            if (amplitudes.isEmpty()) return@List 0.1f

            val distance = kotlin.math.abs(index - centerIndex)
            val percentFromCenter = distance.toFloat() / centerIndex.toFloat()

            // Map to source (center is most recent)
            val dataIndex = ((amplitudes.size - 1) * (1f - percentFromCenter)).toInt()
            val rawAmp = amplitudes[dataIndex.coerceIn(0, amplitudes.size - 1)]

            //  Boost the signal so quiet sounds are visible
            //  Exponential falloff: Keep the middle tall, drop off edges
            val boostedAmp = (rawAmp * gain).coerceIn(0.1f, 1.0f)
            val falloff = (1f - percentFromCenter).coerceIn(0.2f, 1.0f)

            boostedAmp * falloff
        }
    }

    // Faster, tighter animation using Spring.StiffnessMedium
    val animatedAmplitudes = symmetricAmplitudes.map { targetAmp ->
        animateFloatAsState(
            targetValue = targetAmp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium // Much faster/snappier
            ),
            label = "WaveformAnimation"
        ).value
    }

    Canvas(modifier = modifier.height(32.dp)) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val totalBars = animatedAmplitudes.size
        if (totalBars == 0) return@Canvas

        val totalBarWidth = totalBars * barWidth
        val availableSpaceForGaps = canvasWidth - totalBarWidth
        val dynamicGap = if (totalBars > 1) availableSpaceForGaps / (totalBars - 1) else 0f

        animatedAmplitudes.forEachIndexed { index, amp ->
            val x = index * (barWidth + dynamicGap)
            // Ensure even "quiet" bars have a visible minimum height
            val targetHeight = maxOf(canvasHeight * amp, 8f)
            val y = (canvasHeight - targetHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, targetHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}


@Composable
fun PromptIconButton(
    onClick: () -> Unit,
    iconPath: String,
    contentDescription: String,
    hoverBackgroundColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    enabled: Boolean = true,
    iconSize: androidx.compose.ui.unit.Dp = IconSizeSmall
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(SpacingHuge)
            .background(
                // Use hover color if hovered and enabled, otherwise use the resting background color
                color = if (isHovered && enabled) hoverBackgroundColor else backgroundColor,
                shape = CircleShape
            )
            .clip(CircleShape)
            .hoverable(interactionSource)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconPath),
            contentDescription = contentDescription,
            tint = iconTint.copy(alpha = if (enabled) 1f else 0.5f),
            modifier = Modifier.size(iconSize)
        )
    }
}