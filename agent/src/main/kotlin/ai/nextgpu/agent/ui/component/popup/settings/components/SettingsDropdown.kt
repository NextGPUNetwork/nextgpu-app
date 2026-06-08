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
                    .widthIn(min = 120.dp) // Ensures a consistent clickable area
                    .clip(RoundedCornerShape(RadiusMedium)) // THEME: 12.dp
                    .border(
                        width = BorderWidth, // THEME: 0.5.dp
                        color = if (expanded || isHovered) NextGpuTheme.colors.hoverBackground else NextGpuTheme.colors.border,
                        shape = RoundedCornerShape(RadiusMedium)
                    )
                    .background(if (isHovered) NextGpuTheme.colors.hoverBackground else Color.Transparent)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) { expanded = !expanded }
                    .padding(horizontal = SpacingMedium, vertical = SpacingSmall), // THEME: 10.dp, 8.dp
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selectedValue,
                    color = NextGpuTheme.colors.textPrimary,
                    style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.width(SpacingSmall))

                Icon(
                    painter = painterResource("icons/arrow-down.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp) // Consistent with TopNavigation
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
                    Column(modifier = Modifier.padding(SpacingSmall)) { // THEME: 8.dp
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
