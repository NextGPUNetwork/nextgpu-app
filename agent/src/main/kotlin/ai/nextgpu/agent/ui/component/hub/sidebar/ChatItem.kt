package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*

/**
 * Represents a single row in the chat history list.
 * Handles both navigation (click) and selection (checkbox) modes.
 */
@Composable
fun ChatItem(
    label: String,
    isStarred: Boolean = false,
    isInProject: Boolean = false,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    isActiveSession: Boolean = false,
    onRename: (() -> Unit)? = null,
    onStar: (() -> Unit)? = null,
    onAddToProject: (() -> Unit)? = null,
    onRemoveFromProject: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    // Interaction source required to track hover state manually
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    // Visual active state: Selected (in edit mode) OR Hovered/MenuOpen/Active (in normal mode)
    val shouldHighlight = if (isSelectionMode) isSelected else (isActiveSession || isHovered || isMenuOpen)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightButtonCompact + 5.dp)
            .clip(RoundedCornerShape(RadiusRound))
            .background(
                color = if (shouldHighlight) NextGpuTheme.colors.hoverBackground else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (isSelectionMode) {
                CustomRoundedCheckbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = SpacingSmall)
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Normal
            )
        }

        // Context Menu (Only visible on hover/open in Normal Mode)
        if (!isSelectionMode && (isHovered || isMenuOpen)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Quick Star Toggle Button
                SidebarIconButton(
                    icon = if (isStarred) "star-filled" else "star",
                    onClick = { onStar?.invoke() },
                    tint = if (isStarred) StarGolden else NextGpuTheme.colors.textSecondary,
                    iconSize = IconSizeSmall
                )
                Box {
                    SidebarIconButton(
                        icon = "dots-vertical",
                        onClick = { isMenuOpen = true },
                        tint = NextGpuTheme.colors.textSecondary,
                        iconSize = IconSizeSmall
                    )

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            medium = RoundedCornerShape(RadiusMedium)
                        )
                    ) {
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            modifier = Modifier
                                .background(NextGpuTheme.colors.background)
                                .border(
                                    BorderWidth,
                                    NextGpuTheme.colors.border,
                                    RoundedCornerShape(RadiusMedium)
                                )
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                                StyledMenuItem(
                                    icon = if (isInProject) "folder-minus" else "folder-plus",
                                    text = if (isInProject) "Remove from Project" else "Add to project",
                                    tint = NextGpuTheme.colors.textPrimary,
                                    onClick = {
                                        isMenuOpen = false
                                        if (isInProject) {
                                            onRemoveFromProject?.invoke()
                                        } else {
                                            onAddToProject?.invoke()
                                        }
                                    })
                                StyledMenuItem(
                                    icon = "pencil-edit",
                                    text = "Rename",
                                    tint = NextGpuTheme.colors.textPrimary,
                                    onClick = {
                                        isMenuOpen = false
                                        onRename?.invoke()
                                    }
                                )
                                Divider(
                                    color = NextGpuTheme.colors.border,
                                    thickness = BorderWidth,
                                    modifier = Modifier.padding(vertical = SpacingTiny)
                                )
                                StyledMenuItem(
                                    icon = "trash",
                                    text = "Delete",
                                    tint = NextGpuTheme.colors.error,
                                    isDestructive = true,
                                    onClick = {
                                        isMenuOpen = false
                                        onDelete?.invoke()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
