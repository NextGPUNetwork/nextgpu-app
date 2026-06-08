package ai.nextgpu.agent.ui.component.popup.settings.openclaw

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.nextgpu.agent.ui.theme.*

@Composable
fun ExpandableStepCard(
    stepNumber: Int,
    title: String,
    description: String,
    iconPath: String,
    isActive: Boolean,
    isDone: Boolean,
    isExpanded: Boolean,
    isOverview: Boolean,
    onToggleExpand: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val bgColor = if (isOverview) NextGpuTheme.colors.surface.copy(alpha = 0.5f) else Color.Transparent
    val borderColor = if (isOverview) NextGpuTheme.colors.border else Color.Transparent
    val cardPadding = if (isOverview) SpacingLarge else 0.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(RadiusLarge))
            .border(BorderWidth, borderColor, RoundedCornerShape(RadiusLarge))
            .padding(cardPadding)
    ) {
        if (isOverview) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(OpenclawRed.copy(alpha = 0.1f), RoundedCornerShape(RadiusMedium)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(painter = painterResource(iconPath), contentDescription = null, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(SpacingLarge))
                Column {
                    Text("$title", style = MaterialTheme.typography.h5.copy(fontWeight = FontWeight.Medium), color = NextGpuTheme.colors.textPrimary, modifier = Modifier.padding(bottom = SpacingTiny))
                    Text(description, style = MaterialTheme.typography.body2, color = NextGpuTheme.colors.textSecondary, lineHeight = 20.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = SpacingMedium)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (isActive || isDone) onToggleExpand()
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .let {
                                if (isDone) it.background(OpenclawRed.copy(alpha = 0.15f), RoundedCornerShape(50))
                                else if (isActive) it.background(OpenclawRed.copy(alpha = 0.5f), RoundedCornerShape(50))
                                else it.border(2.dp, NextGpuTheme.colors.border, RoundedCornerShape(50))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) Icon(Icons.Default.Check, contentDescription = "Done", tint = OpenclawRed, modifier = Modifier.size(18.dp))
                        else if (isActive) Box(modifier = Modifier.size(10.dp).background(Primary03Black, RoundedCornerShape(50)))
                    }

                    Spacer(modifier = Modifier.width(SpacingMedium))

                    Text(
                        text = title,
                        style = MaterialTheme.typography.h6.copy(fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal),
                        color = if (isActive) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary
                    )
                }

                if (isActive || isDone) {
                    val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(NextGpuTheme.colors.surface, RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .clickable { onToggleExpand() },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource("icons/arrow-down.svg"),
                            contentDescription = "Toggle",
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer { rotationZ = rotation },
                            colorFilter = ColorFilter.tint(NextGpuTheme.colors.textPrimary)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(modifier = Modifier.padding(bottom = SpacingMedium, start = 32.dp + SpacingMedium)) {
                    content()
                }
            }
        }
    }
}