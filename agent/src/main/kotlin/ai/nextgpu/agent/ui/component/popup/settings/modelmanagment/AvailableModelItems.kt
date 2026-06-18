package ai.nextgpu.agent.ui.component.popup.settings.modelmanagment

import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.IconPosition
import ai.nextgpu.agent.ui.component.popup.settings.AiModelExpandedDetails
import ai.nextgpu.common.dto.AiModelDto
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusMedium
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.agent.ui.theme.ElevationMicro
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight

@Composable
fun AvailableModelItem(
    model: AiModelDto,
    isDownloading: Boolean = false,
    isPaused: Boolean = false,
    isStopping: Boolean = false,
    downloadProgress: Double = 0.0,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onStopDownload: () -> Unit
) {
    val isActive = isDownloading || isPaused || isStopping || isExpanded
    val itemShape = RoundedCornerShape(RadiusMedium)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val dynamicBackgroundColor = when {
        isActive -> NextGpuTheme.colors.backgroundVariant
        isHovered -> NextGpuTheme.colors.hoverBackground
        else -> Color.Transparent
    }

    // NEW: Clean text formatter for type keys like "image_generation" -> "Image Generation"
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
        border = if (isActive) BorderStroke(ElevationMicro, NextGpuTheme.colors.border) else null,
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
                        // FIXED: Enforced capitalized GB and mapped the type string format
                        text = "${model.sizeInGB} GB • $formattedType",
                        style = MaterialTheme.typography.caption,
                        color = NextGpuTheme.colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(SpacingMedium))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        isStopping -> {
                            Text(
                                text = "Cancelling...",
                                style = MaterialTheme.typography.caption,
                                color = NextGpuTheme.colors.textSecondary
                            )
                        }

                        isDownloading || isPaused -> {
                            if (isDownloading && !isPaused) {
                                CustomButton(
                                    text = "Pause",
                                    icon = painterResource("icons/pause.svg"),
                                    iconPosition = IconPosition.Start,
                                    onClick = onPauseDownload,
                                    backgroundColor = NextGpuTheme.colors.surface,
                                    textColor = NextGpuTheme.colors.textPrimary,
                                    borderColor = NextGpuTheme.colors.border,
                                    elevation = false,
                                    contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = 6.dp)
                                )
                            } else if (isPaused) {
                                CustomButton(
                                    text = "Resume",
                                    icon = painterResource("icons/resume.svg"),
                                    iconPosition = IconPosition.Start,
                                    onClick = onResumeDownload,
                                    backgroundColor = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f),
                                    textColor = NextGpuTheme.colors.primaryVariant,
                                    elevation = false,
                                    contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Cancel",
                                icon = painterResource("icons/stop.svg"),
                                iconPosition = IconPosition.Start,
                                onClick = onStopDownload,
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                borderColor = NextGpuTheme.colors.border,
                                hoverBackgroundColor = NextGpuTheme.colors.error.copy(alpha = 0.1f),
                                hoverTextColor = NextGpuTheme.colors.error,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = 6.dp)
                            )
                        }

                        else -> {
                            IconButton(onClick = onDownload) {
                                Icon(
                                    painter = painterResource("icons/download.svg"),
                                    contentDescription = "Download Model",
                                    tint = NextGpuTheme.colors.primaryVariant,
                                    modifier = Modifier.size(IconSizeSmall)
                                )
                            }
                        }
                    }
                }
            }

            if (isDownloading || isPaused) {
                Spacer(modifier = Modifier.height(SpacingMedium))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val safeProgress = remember(downloadProgress) { downloadProgress.toFloat().coerceIn(0f, 1f) }
                    val indicatorColor = if (isPaused) NextGpuTheme.colors.textSecondary else NextGpuTheme.colors.primaryVariant

                    SimpleProgressBar(
                        progress = safeProgress,
                        modifier = Modifier.weight(1f),
                        color = indicatorColor,
                        backgroundColor = NextGpuTheme.colors.border
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    Text(
                        text = "${(safeProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.caption,
                        color = NextGpuTheme.colors.textSecondary,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                AiModelExpandedDetails(model)
            }
        }
    }
}

@Composable
fun SimpleProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color,
    backgroundColor: Color
) {
    Box(modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor) // The track
    ) {
        Box(modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f)) // The fill
                .background(color)
        )
    }
}