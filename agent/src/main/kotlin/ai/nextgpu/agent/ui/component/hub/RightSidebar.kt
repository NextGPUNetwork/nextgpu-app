package ai.nextgpu.agent.ui.component.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.ui.theme.*

@Composable
fun RightSidebar(
    isOpen: Boolean,
    onClose: () -> Unit,
    pinnedMessages: List<ChatMessage>,
    onUnpin: (Long) -> Unit,
    onMessageClick: (ChatMessage) -> Unit // NEW: Callback for navigation
) {
    AnimatedVisibility(
        visible = isOpen,
        enter = expandHorizontally(expandFrom = Alignment.Start),
        exit = shrinkHorizontally(shrinkTowards = Alignment.Start)
    ) {
        val borderColor = NextGpuTheme.colors.border
        Column(
            modifier = Modifier
                .width(RightSidebarWidth)
                .fillMaxHeight()
                .background(NextGpuTheme.colors.backgroundVariant)
                .drawWithContent {
                    drawContent()
                    val strokeWidth = BorderWidth.toPx()
                    val x = strokeWidth / 2
                    drawLine(
                        color = borderColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(SpacingMedium)
        ) {
            // --- Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeightTopBar)
                    .padding(bottom = SpacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context Panel",
                    style = MaterialTheme.typography.h6,
                    color = NextGpuTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                ChatActionButton(
                    icon = "sidebar-right",
                    description = "Close Sidebar",
                    onClick = onClose
                )
            }

            // --- Content Area ---
            val listState = rememberLazyListState()

            Box(modifier = Modifier.weight(1f)) {
                if (pinnedMessages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pinned messages.",
                            color = NextGpuTheme.colors.textSecondary,
                            style = MaterialTheme.typography.body2
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(SpacingMedium),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = SpacingSmall)
                    ) {
                        item {
                            Text(
                                text = "Pinned Messages (${pinnedMessages.size}/10)",
                                style = MaterialTheme.typography.subtitle2,
                                color = NextGpuTheme.colors.textPrimary,
                                modifier = Modifier.padding(bottom = SpacingSmall)
                            )
                        }
                        items(pinnedMessages) { message ->
                            RightSidebarPinnedItem(
                                message = message,
                                onUnpin = onUnpin,
                                onClick = { onMessageClick(message) } // NEW: Pass the click
                            )
                        }
                    }
                }

                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(listState),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    style = ScrollbarStyle(
                        minimalHeight = 64.dp,
                        thickness = 6.dp,
                        shape = RoundedCornerShape(RadiusSmall),
                        hoverDurationMillis = 0,
                        unhoverColor = Secondary03LightGray,
                        hoverColor = Secondary04DarkGray
                    )
                )
            }
        }
    }
}

@Composable
private fun RightSidebarPinnedItem(
    message: ChatMessage,
    onUnpin: (Long) -> Unit,
    onClick: () -> Unit // NEW: Click parameter
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(NextGpuTheme.colors.background)
            .clickable(onClick = onClick) // NEW: Make the card clickable
            .padding(SpacingMedium)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = message.role.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.body2,
                color =  NextGpuTheme.colors.textSecondary,
                fontWeight = FontWeight.Bold
            )

            ChatActionButton(
                icon = "pin",
                description = "Unpin",
                onClick = { message.id?.let { onUnpin(it) } },
                tint = NextGpuTheme.colors.textSecondary,
            )
        }

        Spacer(modifier = Modifier.height(SpacingSmall))

        Text(
            text = message.content,
            style = MaterialTheme.typography.body2,
            color = NextGpuTheme.colors.textSecondary,
            maxLines = 6,
            overflow = TextOverflow.Ellipsis
        )
    }
}
