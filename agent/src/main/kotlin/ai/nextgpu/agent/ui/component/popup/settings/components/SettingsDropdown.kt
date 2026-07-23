package ai.nextgpu.agent.ui.component.popup.settings.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SettingsDropdown(
    title: String,
    description: String?,
    options: List<String>,
    selectedValue: String,
    onOptionSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val rotation by animateFloatAsState(if (expanded) 180f else 0f)

    SettingsItemRow(title = title, description = description) {
        Box {
            // --- 1. Trigger Button (Styled like TopNavigation) ---
            Row(
                modifier = Modifier
                    .widthIn(min = 100.dp, max = 120.dp) // Kept your sizing logic, but you can change this to .width(140.dp) if you want exact width matching
                    .clip(RoundedCornerShape(RadiusRound)) // Matched UI: Pill shape
                    .background(if (isHovered) NextGpuTheme.colors.hoverBackground else NextGpuTheme.colors.backgroundVariant) // Matched UI: Solid background, no border
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) { expanded = !expanded } // Kept functionality
                    .padding(horizontal = SpacingMedium, vertical = SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
                // Removed horizontalArrangement = Arrangement.SpaceBetween because weight(1f) on text handles the spacing natively now
            ) {
                Text(
                    text = selectedValue,
                    color = NextGpuTheme.colors.textPrimary,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1, // Matched UI: Prevent multi-line stretching
                    overflow = TextOverflow.Ellipsis, // Matched UI: Clean truncation
                    modifier = Modifier.weight(1f) // Matched UI: Takes up remaining space and pushes icon to the right
                )

                Spacer(modifier = Modifier.width(SpacingSmall))

                Icon(
                    painter = painterResource("icons/arrow-down.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(rotation),
                    tint = NextGpuTheme.colors.textSecondary
                )
            }

            // --- 2. Dropdown Menu (Styled like TopNavigation) ---
            // Override MaterialTheme shapes locally for this menu
            MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(NextGpuTheme.colors.background)
                        .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
                ) {
                    Column(modifier = Modifier.padding(horizontal = SpacingSmall)) { // THEME: 8.dp
                        options.forEach { option ->
                            val isSelected = option == selectedValue

                            StyledSettingsMenuItem(
                                onClick = {
                                    onOptionSelect(option)
                                    expanded = false
                                },
                                isSelected = isSelected
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.body2,
                                    color = NextGpuTheme.colors.textPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Menu Item Styled like TopNavigation
 */
@Composable
private fun StyledSettingsMenuItem(
    onClick: () -> Unit,
    isSelected: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val backgroundColor = when {
        isSelected -> NextGpuTheme.colors.hoverBackground
        isHovered -> NextGpuTheme.colors.hoverBackground.copy(alpha = 0.5f)
        else -> Color.Transparent
    }

    DropdownMenuItem(
        onClick = onClick,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = RadiusMedium),
        modifier = Modifier
            .padding(vertical = SpacingMicro) // THEME: 2.dp
            .height(HeightButtonCompact) // THEME: 32.dp
            .clip(RoundedCornerShape(RadiusSmall)) // THEME: 6.dp
            .background(backgroundColor),
        content = content
    )
}
