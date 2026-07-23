package ai.nextgpu.agent.ui.component.popup.settings.modelmanagment

import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.IconPosition
import ai.nextgpu.agent.ui.component.popup.settings.AiModelExpandedDetails
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusMedium
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.common.dto.AiModelDto
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ========================================================
// INSTALLED MODEL ITEM
// ========================================================
@Composable
fun InstalledModelItem(
    model: AiModelDto,
    isDeleting: Boolean = false,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val itemShape = RoundedCornerShape(RadiusMedium)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val showDelete = isHovered || isDeleting || isExpanded

    val dynamicBackgroundColor = if (isHovered && !isDeleting && !isExpanded) {
        NextGpuTheme.colors.hoverBackground
    } else {
        Color.Transparent
    }

    val formattedType = remember(model.type) {
        model.type.split("_", "-")
            .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(itemShape)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggleExpand() },
        shape = itemShape,
        backgroundColor = dynamicBackgroundColor,
        border = null,
        elevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(SpacingMedium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.fullName ?: model.model,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.Bold,
                        color = NextGpuTheme.colors.textPrimary
                    )
                    Text(
                        text = "${model.sizeInGB} GB • $formattedType",
                        style = MaterialTheme.typography.caption,
                        color = NextGpuTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(SpacingMedium))

                // Redesigned Delete Control Area matching the Cancel look & structure
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.alpha(if (showDelete) 1f else 0f)
                ) {
                    if (isDeleting) {
                        // Instead of rotating the whole button, we remove the modifier from CustomButton
                        // and rely on the fact that the 'icon' painter is already passed in.
                        // If CustomButton doesn't support modifier rotation,
                        // we use a separate Row to hold the button and the rotating icon.

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Just the rotating icon
                            Icon(
                                painter = painterResource("icons/loading.svg"),
                                contentDescription = null,
                                tint = NextGpuTheme.colors.textSecondary,
                                modifier = Modifier.size(IconSizeSmall).rotate(rotation)
                            )
                            Spacer(modifier = Modifier.width(SpacingSmall))
                            Text("Deleting...", color = NextGpuTheme.colors.textSecondary)
                        }
                    } else {
                        // Your standard delete button
                        CustomButton(
                            text = "Delete",
                            icon = painterResource("icons/trash.svg"),
                            iconPosition = IconPosition.Start,
                            onClick = onDelete,
                            backgroundColor = Color.Transparent,
                            textColor = NextGpuTheme.colors.textSecondary,
                            borderColor = NextGpuTheme.colors.border,
                            hoverBackgroundColor = NextGpuTheme.colors.error.copy(alpha = 0.1f),
                            hoverTextColor = NextGpuTheme.colors.error,
                            elevation = false,
                            contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = 6.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                AiModelExpandedDetails(model)
            }
        }
    }
}