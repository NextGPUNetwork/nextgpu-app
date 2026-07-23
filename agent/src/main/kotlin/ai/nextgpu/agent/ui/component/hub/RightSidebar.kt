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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource

@Composable
fun RightSidebar(
    isOpen: Boolean,
    onClose: () -> Unit,
    pinnedMessages: List<ChatMessage>,
    onUnpin: (Long) -> Unit,
    onMessageClick: (ChatMessage) -> Unit // NEW: Callback for navigation
) {
    val sidebarInteraction = remember { MutableInteractionSource() }

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
                    text = "Pinned Messages (${pinnedMessages.size}/10)",
                    style = MaterialTheme.typography.h6,
                    color = NextGpuTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )


                Box(
                    modifier = Modifier
                        .size(IconSizeMedium + 2.dp) // Small, subtle circle matching your mockup
                        .clip(CircleShape)
                        .hoverable(sidebarInteraction)
                        .background(
                            color = NextGpuTheme.colors.hoverBackground,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = sidebarInteraction,
                            indication = ripple(bounded = true),
                            onClick = onClose
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource("icons/arrow-right-no-line.svg"), // Replace with your arrow SVG
                        contentDescription = "Toggle Sidebar",
                        tint = NextGpuTheme.colors.textSecondary,
                        modifier = Modifier
                            .size(12.dp)
                    )
                }
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
                        unhoverColor = NextGpuTheme.colors.background,
                        hoverColor = NextGpuTheme.colors.hoverBackground,
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
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(RadiusMedium))
            .background(NextGpuTheme.colors.background)
            .clickable(onClick = onClick)
            .padding(SpacingMedium),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = message.role.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.body2,
                color = NextGpuTheme.colors.textSecondary,
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

        // --- MARKDOWN PREVIEW WRAPPER ---

        // 1. Create a "Mini" Typography scale just for this preview card
        val miniTypography = MaterialTheme.typography.copy(
            h1 = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.Bold),
            h2 = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold),
            h3 = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
            body1 = MaterialTheme.typography.caption,
            body2 = MaterialTheme.typography.caption
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 100.dp) // Constrain the absolute maximum height of the markdown
                // 2. The Fading Edge Magic
                .graphicsLayer { alpha = 0.99f } // Required to create an offscreen buffer for the blend mode
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.6f to androidx.compose.ui.graphics.Color.Black, // Solid until 60% down
                            1.0f to androidx.compose.ui.graphics.Color.Transparent // Fades to transparent at the bottom
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                    )
                }
        ) {
            // Apply the scaled-down text sizes locally
            MaterialTheme(typography = miniTypography) {
                ResponseHandler(
                    content = message.content.trim(), // Keep the trim!
                    textColor = NextGpuTheme.colors.textSecondary
                )
            }

            // 3. Click Absorber Overlay
            // This prevents Markdown links or code blocks from stealing the click from the card
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}