package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.model.ChatSession
import ai.nextgpu.agent.model.Project
import ai.nextgpu.agent.ui.theme.*

/**
 * Represents a project in the sidebar, which can be expanded to show its chats.
 */
@Composable
fun ProjectItem(
    project: Project,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isSelectionMode: Boolean,
    selectedSessionIds: Set<Long>,
    onSessionClick: (ChatSession) -> Unit,
    onAddToProject: (ChatSession) -> Unit,
    onRemoveFromProject: (ChatSession) -> Unit,
    onDeleteSession: (ChatSession) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isMenuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HeightButtonCompact)
                .clip(RoundedCornerShape(RadiusSmall))
                .background(if (isHovered || isMenuOpen) NextGpuTheme.colors.hoverBackground else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                )
                .padding(horizontal = SpacingSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(if (isExpanded) "icons/arrow-down.svg" else "icons/arrow-right-no-line.svg"),
                contentDescription = null,
                modifier = Modifier.size(IconSizeMicro),
                tint = NextGpuTheme.colors.textSecondary // THEME FIX: Replaced PrimaryText02
            )
            Spacer(modifier = Modifier.width(SpacingSmall))
            Text(
                text = project.name ?: "Untitled Project",
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isHovered || isMenuOpen) {
                Box {
                    SidebarIconButton(
                        icon = "dots-horizontal",
                        onClick = { isMenuOpen = true },
                        tint = NextGpuTheme.colors.textSecondary, // THEME FIX: Replaced SecondaryText02
                        iconSize = IconSizeSmall
                    )

                    // THEME FIX: Wrapped in MaterialTheme to enforce rounded corners properly
                    MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
                        DropdownMenu(
                            expanded = isMenuOpen,
                            onDismissRequest = { isMenuOpen = false },
                            modifier = Modifier
                                .background(NextGpuTheme.colors.background) // THEME FIX: Replaced Primary01White
                                .border(
                                    BorderWidth,
                                    NextGpuTheme.colors.border, // THEME FIX: Replaced StrokeGray
                                    RoundedCornerShape(RadiusMedium)
                                )
                        ) {
                            Column(modifier = Modifier.padding(SpacingSmall)) {
                                StyledMenuItem(
                                    icon = "pencil-edit",
                                    text = "Edit",
                                    tint = NextGpuTheme.colors.textSecondary,
                                    onClick = { isMenuOpen = false; onRename() }
                                )
                                Divider(
                                    color = NextGpuTheme.colors.border, // THEME FIX: Replaced StrokeGray
                                    thickness = BorderWidth,
                                    modifier = Modifier.padding(vertical = SpacingTiny)
                                )
                                StyledMenuItem(
                                    icon = "trash",
                                    text = "Delete",
                                    isDestructive = true,
                                    onClick = { isMenuOpen = false; onDelete() }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(start = SpacingLarge)) {
                val chats = project.chatSessions ?: emptyList()
                if (chats.isEmpty()) {
                    Text(
                        text = "No chats in this project",
                        style = MaterialTheme.typography.caption,
                        color = NextGpuTheme.colors.textSecondary, // THEME FIX: Replaced Secondary04DarkGray
                        modifier = Modifier.padding(vertical = SpacingMicro)
                    )
                } else {
                    chats.forEach { session ->
                        val label =
                            session.name ?: session.messages.firstOrNull { it.role == "user" }?.content?.take(20)
                            ?: "Empty Chat"
                        ChatItem(
                            label = label,
                            isStarred = session.starred,
                            isInProject = true,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedSessionIds.contains(session.id),
                            onAddToProject = { onAddToProject(session) },
                            onRemoveFromProject = { onRemoveFromProject(session) },
                            onDelete = { onDeleteSession(session) },
                            onClick = { onSessionClick(session) }
                        )
                    }
                }
            }
        }
    }
}