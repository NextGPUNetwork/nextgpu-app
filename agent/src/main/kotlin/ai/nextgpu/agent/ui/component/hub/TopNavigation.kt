package ai.nextgpu.agent.ui.component.hub

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ai.nextgpu.agent.model.PromptModel
import ai.nextgpu.agent.ui.AppPortal
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.theme.*

@Composable
fun TopNavigation(
    currentMode: PromptMode, // <--- ADD THIS NEW PARAMETER
    isSidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    userMenuOpen: Boolean,
    onUserMenuOpenChange: (Boolean) -> Unit,
    modelMenuOpen: Boolean,
    onModelMenuOpenChange: (Boolean) -> Unit,
    modelOptions: List<PromptModel>,
    selectedModel: String?,
    onSelectModel: (String) -> Unit,
    hasActiveSession: Boolean,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    isCaseSensitive: Boolean,
    onCaseSensitiveChange: (Boolean) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onNextMatch: () -> Unit,
    onPrevMatch: () -> Unit,
    isPrivateMode: Boolean,
    onPrivateModeChange: (Boolean) -> Unit,
    hasPinnedMessages: Boolean,
    onToggleRightSidebar: () -> Unit,
    isRightSidebarOpen: Boolean,
) {
    var showModelChangeWarning by remember { mutableStateOf(false) }
    var pendingModelSelection by remember { mutableStateOf<String?>(null) }

    // --- NEW LOGIC: Determine behavior based on mode ---
    val isImageMode = currentMode == PromptMode.IMAGE
    // Image models can be changed mid-session. Text models cannot.
    val isSelectorClickable = !hasActiveSession || isImageMode

    val displayText = when {
        selectedModel != null -> selectedModel
        isImageMode && modelOptions.isEmpty() -> "No models available"
        else -> "Select Model"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightTopBar)
            .background(NextGpuTheme.colors.background)
            .clip(RoundedCornerShape(topStart = RadiusMedium))
            .padding(horizontal = SpacingMedium)
    ) {
        // --- Left side: Model Selector ---
        Box(
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            val rotation by animateFloatAsState(if (modelMenuOpen) 180f else 0f)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusRound))
                    .background(if (isSelectorClickable && isHovered) NextGpuTheme.colors.hoverBackground else NextGpuTheme.colors.backgroundVariant)
                    .clickable(
                        enabled = isSelectorClickable, // Use our new logic here
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) { onModelMenuOpenChange(!modelMenuOpen) }
                    .padding(horizontal = SpacingMedium, vertical = SpacingSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayText, // Use our dynamic text here
                    color = if (!isSelectorClickable) NextGpuTheme.colors.textSecondary else NextGpuTheme.colors.textPrimary,
                    style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Medium)
                )

                Spacer(modifier = Modifier.width(SpacingSmall))

                Icon(
                    painter = painterResource("icons/arrow-down.svg"),
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(rotation),
                    tint = if (!isSelectorClickable) Color.Transparent else NextGpuTheme.colors.textSecondary
                )
            }

            MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
                DropdownMenu(
                    expanded = modelMenuOpen,
                    onDismissRequest = { onModelMenuOpenChange(false) },
                    modifier = Modifier
                        .background(NextGpuTheme.colors.background)
                        .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
                ) {
                    Column(modifier = Modifier.padding(SpacingSmall)) {

                        // Handle Empty State gracefully
                        if (isImageMode && modelOptions.isEmpty()) {
                            DropdownMenuItem(
                                onClick = { onModelMenuOpenChange(false) },
                                contentPadding = PaddingValues(horizontal = RadiusMedium)
                            ) {
                                Text(
                                    text = "Please download an image model from Settings",
                                    style = MaterialTheme.typography.body2,
                                    color = NextGpuTheme.colors.textSecondary
                                )
                            }
                        } else {
                            modelOptions.forEach { option ->
                                val isSelected = option.name == selectedModel

                                StyledDropdownMenuItem(
                                    onClick = {
                                        // NEW LOGIC: Bypass warning if we are just swapping Image Models
                                        if (isImageMode) {
                                            onSelectModel(option.name)
                                        } else if (hasActiveSession && option.name != selectedModel) {
                                            pendingModelSelection = option.name
                                            showModelChangeWarning = true
                                        } else {
                                            onSelectModel(option.name)
                                        }
                                        onModelMenuOpenChange(false)
                                    },
                                    isSelected = isSelected
                                ) {
                                    Text(
                                        text = option.name,
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

        // --- Middle: Search Bar ---
        if (hasActiveSession) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    // The 180dp symmetric padding reserves space for the left and right controls.
                    // This guarantees absolute centering while forcing the search bar to shrink on smaller screens.
                    .padding(horizontal = 180.dp)
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 500.dp)
                        .height(HeightButtonCompact)
                        .clip(RoundedCornerShape(RadiusRound))
                        .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusRound))
                        .padding(horizontal = SpacingSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource("icons/search.svg"),
                        contentDescription = "Search",
                        tint = NextGpuTheme.colors.textSecondary,
                        modifier = Modifier.size(IconSizeMedium)
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    Box(modifier = Modifier.weight(1f)) {
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Search messages...",
                                style = MaterialTheme.typography.body2,
                                color = NextGpuTheme.colors.textSecondary
                            )
                        }
                        BasicTextField(
                            value = searchText,
                            onValueChange = onSearchTextChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.body2.copy(color = NextGpuTheme.colors.textPrimary),
                            cursorBrush = SolidColor(NextGpuTheme.colors.textPrimary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (matchCount > 0) {
                        Spacer(modifier = Modifier.width(SpacingSmall))

                        Text(
                            text = "${currentMatchIndex + 1}/${matchCount} chats",
                            style = MaterialTheme.typography.caption,
                            color = SecondaryText01,
                            modifier = Modifier.padding(end = SpacingTiny)
                        )

                        Divider(
                            color = NextGpuTheme.colors.border,
                            modifier = Modifier
                                .height(16.dp)
                                .width(BorderWidth)
                        )

                        Row(
                            modifier = Modifier.padding(horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onNextMatch,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Next Result",
                                    tint = NextGpuTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onPrevMatch,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Previous Result",
                                    tint = NextGpuTheme.colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Divider(
                            color = NextGpuTheme.colors.border,
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    val caseInteractionSource = remember { MutableInteractionSource() }
                    val isCaseHovered by caseInteractionSource.collectIsHoveredAsState()

                    Box(
                        modifier = Modifier
                            .height(SpacingExtraLarge)
                            .widthIn(min = SpacingMassive)
                            .clip(RoundedCornerShape(RadiusRound))
                            .background(
                                color = when {
                                    isCaseSensitive -> Primary02Purple.copy(alpha = 0.2f)
                                    isCaseHovered -> NextGpuTheme.colors.hoverBackground
                                    else -> Color.Transparent
                                }
                            )
                            .hoverable(caseInteractionSource)
                            .clickable(
                                interactionSource = caseInteractionSource,
                                indication = ripple(bounded = true)
                            ) { onCaseSensitiveChange(!isCaseSensitive) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aa",
                            style = MaterialTheme.typography.caption,
                            color = if (isCaseSensitive) Primary02Purple else NextGpuTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }

        // --- Right Side Controls (Pin & User Avatar) ---
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            if (hasActiveSession) {
                val isActive = isRightSidebarOpen
                val pinInteraction = remember { MutableInteractionSource() }
                val isPinHovered by pinInteraction.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(IconSizeLarge)
                        .clip(CircleShape)
                        .hoverable(pinInteraction)
                        .background(
                            color = if (isActive) NextGpuTheme.colors.textSecondary
                            else if (isPinHovered) NextGpuTheme.colors.hoverBackground
                            else NextGpuTheme.colors.backgroundVariant,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = pinInteraction,
                            indication = ripple(bounded = true),
                            onClick = onToggleRightSidebar
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource("icons/pin-filled.svg"),
                        contentDescription = "View Pinned",
                        tint = if (isActive) NextGpuTheme.colors.background
                        else if (hasPinnedMessages) Primary02Purple
                        else NextGpuTheme.colors.textSecondary,
                        modifier = Modifier.size(IconSizeMedium)
                    )
                }
            }

            Box {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val isActive = userMenuOpen || isHovered

                val privateInteractionSource = remember { MutableInteractionSource() }
                val isPrivateHovered by privateInteractionSource.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(IconSizeLarge)
                        .clip(CircleShape)
                        .hoverable(privateInteractionSource)
                        .background(
                            color = if (isPrivateMode) NextGpuTheme.colors.textSecondary
                            else if (isPrivateHovered) NextGpuTheme.colors.hoverBackground
                            else NextGpuTheme.colors.backgroundVariant,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = privateInteractionSource,
                            indication = ripple(bounded = true)
                        ) { onPrivateModeChange(!isPrivateMode) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource( "icons/incognito-profile.svg"),
                        contentDescription = "Toggle Private Mode",
                        modifier = Modifier.size(IconSizeMedium),
                        // If active, invert the color to match the background, just like the pin button
                        tint = if (isPrivateMode) NextGpuTheme.colors.background else NextGpuTheme.colors.textSecondary,
                    )
                }

//                if (!isPrivateMode) {
//                    MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
//                        DropdownMenu(
//                            expanded = userMenuOpen,
//                            onDismissRequest = { onUserMenuOpenChange(false) },
//                            modifier = Modifier
//                                .background(NextGpuTheme.colors.background)
//                                .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
//                        ) {
//                            Column(modifier = Modifier.padding(SpacingSmall)) {
//                                Column(
//                                    modifier = Modifier
//                                        .padding(horizontal = SpacingTiny, vertical = SpacingSmall)
//                                ) {
//                                    Row(verticalAlignment = Alignment.CenterVertically) {
//                                        Column {
//                                            Text(
//                                                text = "Guest user",
//                                                color = NextGpuTheme.colors.textPrimary,
//                                                style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold)
//                                            )
//                                            Text(
//                                                text = "0x0000...0000",
//                                                color = PrimaryText02,
//                                                style = MaterialTheme.typography.caption
//                                            )
//                                        }
//                                    }
//                                }
//
//                                Divider(color = NextGpuTheme.colors.border, thickness = BorderWidth, modifier = Modifier.padding(vertical = SpacingTiny))
//
//                                StyledDropdownMenuItem(onClick = { onUserMenuOpenChange(false) }) {
//                                    Text("Balance: 0 NGPU", color = NextGpuTheme.colors.textPrimary, style = MaterialTheme.typography.body2)
//                                }
//                                StyledDropdownMenuItem(onClick = { onUserMenuOpenChange(false) }) {
//                                    Text("Privacy Mode", color = NextGpuTheme.colors.textPrimary, style = MaterialTheme.typography.body2)
//                                }
//                                StyledDropdownMenuItem(onClick = { onUserMenuOpenChange(false) }) {
//                                    Text("Sign out", color = ErrorText, style = MaterialTheme.typography.body2)
//                                }
//                            }
//                        }
//                    }
//                }
            }
        }
    }

    // --- Model Change Warning Dialog ---
    if (showModelChangeWarning) {
        AppPortal {
            Dialog(
                onDismissRequest = {
                    showModelChangeWarning = false
                    pendingModelSelection = null
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingLarge,
                            start = SpacingLarge,
                            end = SpacingLarge,
                            bottom = SpacingMedium
                        )
                    ) {
                        Text(
                            text = "Switching Model?",
                            style = NextGpuTheme.typography.h6,
                            color = NextGpuTheme.colors.textPrimary,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "Changing the AI model will reset your current chat session. Do you want to proceed?",
                            style = MaterialTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary,
                            modifier = Modifier.padding(bottom = SpacingMedium)
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                text = "Cancel",
                                onClick = {
                                    showModelChangeWarning = false
                                    pendingModelSelection = null
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Yes, Switch Model",
                                onClick = {
                                    showModelChangeWarning = false
                                    pendingModelSelection?.let { onSelectModel(it) }
                                    pendingModelSelection = null
                                },
                                backgroundColor = ErrorText,
                                textColor = Primary01White,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
private fun StyledDropdownMenuItem(
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
            .padding(vertical = SpacingMicro)
            .height(HeightButtonCompact)
            .clip(RoundedCornerShape(RadiusSmall))
            .background(backgroundColor),
        content = content
    )
}