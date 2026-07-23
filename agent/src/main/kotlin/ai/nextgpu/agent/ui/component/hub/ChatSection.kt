package ai.nextgpu.agent.ui.component.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.model.ChatMessage
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.theme.BorderWidth
import ai.nextgpu.agent.ui.theme.ElevationNone
import ai.nextgpu.agent.ui.theme.MaxContentWidth
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusExtraLarge
import ai.nextgpu.agent.ui.theme.RadiusMedium
import ai.nextgpu.agent.ui.theme.RadiusSmall
import ai.nextgpu.agent.ui.theme.Secondary03LightGray
import ai.nextgpu.agent.ui.theme.Secondary04DarkGray
import ai.nextgpu.agent.ui.theme.SpacingLarge
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.agent.ui.theme.SpacingTiny
import ai.nextgpu.agent.ui.theme.WarnText
import org.springframework.core.env.Environment
import java.nio.file.Paths
import androidx.compose.foundation.v2.ScrollbarAdapter // Note: depending on your compose version, this might just be androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.offset
import kotlinx.coroutines.launch
@Composable
fun ChatSection(
    messages: List<ChatMessage>,
    onPromptChange: (TextFieldValue) -> Unit,
    onTogglePin: (Long) -> Unit,
    isGenerating: Boolean,
    scrollState: ScrollState, // CHANGED: Replaced LazyListState with ScrollState
    messagePositions: MutableMap<Int, Float>, // ADDED: Tracks Y-coordinates for Search
    searchText: String,
    isCaseSensitive: Boolean,
    activeMessageId: Long?,
    isThinking: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val coroutineScope = rememberCoroutineScope()
    // Determine if we are scrolled up enough to show the button (e.g., 100 pixels away from bottom)
    val showScrollToBottom by remember {
        derivedStateOf {
            scrollState.maxValue - scrollState.value > 100
        }
    }

    // =========================================================
    // CRITICAL SCROLL FIX: Observe the layout bounds directly.
    // Reading `scrollState.maxValue` forces Compose to automatically
    // trigger this effect ONLY after the UI has physically expanded!
    // =========================================================
    val maxScroll = scrollState.maxValue

    LaunchedEffect(messages.size, maxScroll, isGenerating) {
        if (messages.isNotEmpty() && maxScroll > 0) {
            if (isGenerating) {
                // Snap instantly to keep up with fast streaming chunks
                scrollState.scrollTo(maxScroll)
            } else {
                // Smooth scroll for normal user interactions
                scrollState.animateScrollTo(maxScroll)
            }
        }
    }

    Box(
        modifier = Modifier.Companion
            .fillMaxSize()
            .hoverable(interactionSource)
    ) {
        // CHANGED: From LazyColumn to standard Column with verticalScroll
        Column(
            modifier = Modifier.Companion
                .align(Alignment.Companion.TopCenter)
                .widthIn(max = MaxContentWidth)
                .fillMaxWidth()
                .padding(vertical = SpacingMedium)
                .verticalScroll(scrollState), // FLAWLESS SCROLLING APPLIED HERE
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            messages.toList().forEachIndexed { idx, message ->
                ChatBubble(
                    // CHANGED: Now mapping by the 'idx' instead of 'message.id'
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        messagePositions[idx] = coordinates.positionInParent().y
                    },
                    index = idx,
                    message = message,
                    allMessages = messages,
                    onPromptChange = onPromptChange,
                    isPinned = message.pinned ?: false,
                    onTogglePin = { message.id?.let { onTogglePin(it) } },
                    searchText = searchText,
                    isCaseSensitive = isCaseSensitive,
                    isFocused = (message.id != null && message.id == activeMessageId)
                )
            }
            if (isThinking) {
                AssistantThinkingBubble() // Your custom UI or Text("...") styled as an AI bubble
            }
        }

        // BACK TO NATIVE ADAPTER: It now calculates flawlessly because standard Column pre-measures everything!
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.Companion
                .align(Alignment.Companion.CenterEnd)
                .fillMaxHeight()
                .padding(end = SpacingSmall)
                .alpha(if (isHovered) 1f else 0.4f),
            style = ScrollbarStyle(
                minimalHeight = 64.dp,
                thickness = 6.dp,
                shape = RoundedCornerShape(RadiusSmall),
                hoverDurationMillis = 0,
                unhoverColor = NextGpuTheme.colors.textSecondary.copy(0.4F),
                hoverColor = NextGpuTheme.colors.textSecondary
            )
        )
        // =========================================================
        // NEW: FLOATING "SCROLL TO BOTTOM" BUTTON
        // =========================================================
        AnimatedVisibility(
            visible = showScrollToBottom,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = SpacingLarge), // Pushes it slightly above the chat input
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp) // Standard FAB size
                    .background(NextGpuTheme.colors.surface, CircleShape)
                    .border(BorderWidth, NextGpuTheme.colors.border, CircleShape)
                    .clip(CircleShape)
                    .clickable {
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource("icons/arrow-down.svg"),
                    contentDescription = "Scroll to latest messages",
                    tint = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.size(IconSizeSmall) // IconSizeMedium
                )
            }
        }
    }
}

@Composable
fun ChatBubble(
    index: Int,
    message: ChatMessage,
    allMessages: List<ChatMessage>,
    onPromptChange: (TextFieldValue) -> Unit,
    isPinned: Boolean,
    onTogglePin: () -> Unit,
    searchText: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    val env = remember { springContext.getBean(Environment::class.java) }
    val isUser = message.role == "user"
    val clipboard = LocalClipboardManager.current

    // Interaction source to track hover on the entire bubble area
    val bubbleInteractionSource = remember { MutableInteractionSource() }
    val isHovered by bubbleInteractionSource.collectIsHoveredAsState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(bubbleInteractionSource),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = if (isUser) Modifier.widthIn(max = 600.dp) else Modifier.fillMaxWidth()
        ) {
            // The Message Bubble
            Surface(
                color = if (isUser) NextGpuTheme.colors.backgroundVariant else Color.Transparent,
                shape = RoundedCornerShape(RadiusExtraLarge),
                elevation = ElevationNone,
                modifier = Modifier.padding(horizontal = SpacingSmall)
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (isUser) SpacingLarge else 0.dp,
                        vertical = SpacingMedium
                    )
                ) {
                    // UPDATED: Now much cleaner and uses your new component
                    if (message.content.startsWith("<IMG>image<IMG>:")) {
                        ImageMessageContent(content = message.content)
                    }

                    val textContent = message.content.replace("<IMG>image<IMG>:[^\\s]+".toRegex(), "").trim()

                    SelectionContainer {
                        if (isUser) {
                            UserMessageText(
                                content = textContent,
                                searchText = searchText,
                                isCaseSensitive = isCaseSensitive,
                                isFocused = isFocused,
                                modifier = Modifier.wrapContentWidth()
                            )
                        } else {
                            ResponseHandler(
                                content = textContent,
                                textColor = NextGpuTheme.colors.textPrimary,
                                searchText = searchText,
                                isCaseSensitive = isCaseSensitive,
                                isFocused = isFocused
                            )
                        }
                    }
                }
            }

            // The Action Buttons (Below the bubble)
            Row(
                modifier = Modifier
                    .padding(top = SpacingTiny)
                    .padding(horizontal = SpacingSmall),
                horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Define the buttons as reusable blocks to easily swap their order
                val copyButton: @Composable () -> Unit = {
                    ChatActionButton(
                        icon = "copy",
                        description = "Copy to clipboard",
                        onClick = { clipboard.setText(AnnotatedString(message.content)) },
                        // Copy is strictly tied to hover state
                        modifier = Modifier.alpha(if (isHovered) 1f else 0f)
                    )
                }

                val pinButton: @Composable () -> Unit = {
                    ChatActionButton(
                        icon = if (isPinned) "pin-filled" else "pin",
                        description = if (isPinned) "Unpin" else "Pin",
                        onClick = onTogglePin,
                        tint = if (isPinned) NextGpuTheme.colors.primaryVariant else NextGpuTheme.colors.textSecondary,
                        // Pin stays visible if it is pinned OR hovered
                        modifier = Modifier.alpha(if (isHovered || isPinned) 1f else 0f)
                    )
                }

                // Render in standard order for user, reversed for assistant
                if (isUser) {
                    copyButton()
                    pinButton()
                } else {
                    pinButton()
                    copyButton()
                }
            }
        }
    }
}

// Helper composable for Action Buttons with Hover Effect
@Composable
fun ChatActionButton(
    icon: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier, // ADDED: Modifier parameter
    tint: Color = NextGpuTheme.colors.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier // APPLIED: Modifier applied to the outermost Box
            .size(28.dp)
            .clip(RoundedCornerShape(RadiusSmall))
            .hoverable(interactionSource)
            .background(
                color = if (isHovered) Secondary04DarkGray.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(RadiusSmall)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource("icons/$icon.svg"),
            contentDescription = description,
            modifier = Modifier.size(SpacingLarge),
            tint = if (isHovered) NextGpuTheme.colors.textPrimary else tint
        )
    }
}

// Helper composable for Action Buttons with Hover Effect
@Composable
fun ChatActionButton(
    icon: String,
    description: String,
    onClick: () -> Unit,
    tint: Color = NextGpuTheme.colors.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier.Companion
            .size(28.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(RadiusSmall)) // CHANGE: Matches Sidebar hover shape
            .hoverable(interactionSource)
            .background(
                color = if (isHovered) Secondary04DarkGray.copy(alpha = 0.15f) else Color.Companion.Transparent,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(RadiusSmall) // CHANGE: Matches Sidebar hover shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Companion.Center
    ) {
        Icon(
            painter = painterResource("icons/$icon.svg"),
            contentDescription = description,
            modifier = Modifier.Companion.size(SpacingLarge), // THEME: 16dp
            tint = if (isHovered) NextGpuTheme.colors.textPrimary else tint
        )
    }
}

@Composable
fun AssistantThinkingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium),
        horizontalArrangement = Arrangement.Start
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.widthIn(max = 600.dp)
        ) {
            Surface(
                elevation = ElevationNone,
                color = Color.Transparent // Ensures it blends perfectly
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = SpacingLarge, vertical = SpacingMedium),
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // Slightly tighter spacing for graphical dots
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition()

                    // Staggered keyframes create the sequential wave effect
                    val dot1Y by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.keyframes {
                                durationMillis = 1200
                                0f at 0
                                -6f at 200 with androidx.compose.animation.core.FastOutSlowInEasing // Jump up
                                0f at 400 with androidx.compose.animation.core.FastOutSlowInEasing // Fall down
                                0f at 1200 // Rest
                            }
                        )
                    )

                    val dot2Y by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.keyframes {
                                durationMillis = 1200
                                0f at 150 // Wait before jumping
                                -6f at 350 with androidx.compose.animation.core.FastOutSlowInEasing
                                0f at 550 with androidx.compose.animation.core.FastOutSlowInEasing
                                0f at 1200 // Rest
                            }
                        )
                    )

                    val dot3Y by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 0f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = androidx.compose.animation.core.keyframes {
                                durationMillis = 1200
                                0f at 300 // Wait before jumping
                                -6f at 500 with androidx.compose.animation.core.FastOutSlowInEasing
                                0f at 700 with androidx.compose.animation.core.FastOutSlowInEasing
                                0f at 1200 // Rest
                            }
                        )
                    )

                    // Graphical styling: 6dp circles with 60% opacity
                    val dotColor = NextGpuTheme.colors.textSecondary.copy(alpha = 0.6f)
                    val dotModifier = Modifier
                        .size(6.dp)
                        .background(color = dotColor, shape = CircleShape)

                    // Draw the animated geometric dots
                    Box(modifier = Modifier.offset(y = dot1Y.dp).then(dotModifier))
                    Box(modifier = Modifier.offset(y = dot2Y.dp).then(dotModifier))
                    Box(modifier = Modifier.offset(y = dot3Y.dp).then(dotModifier))
                }
            }
        }
    }
}
