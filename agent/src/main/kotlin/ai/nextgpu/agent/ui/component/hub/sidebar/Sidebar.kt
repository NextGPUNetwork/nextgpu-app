package ai.nextgpu.agent.ui.component.hub.sidebar

import ai.nextgpu.agent.model.ChatSession
import ai.nextgpu.agent.model.Project
import ai.nextgpu.agent.service.NextGpuAiService
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Sidebar component handling navigation, chat history, and selection modes.
 *
 * @param isCollapsed Controls the width and visibility of detailed elements.
 * @param onToggleSidebar Callback to toggle the collapsed state.
 * @param onSettings Callback for the settings button.
 */
@Composable
fun Sidebar(
    aiService: NextGpuAiService,
    viewModel: SettingsViewModel,
    refreshTrigger: Int = 0,
    isCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    onSettings: () -> Unit,
    onSessionSelected: (ChatSession) -> Unit,
    onNewChat: () -> Unit,
    activeSessionId: Long? = null
) {

    var coroutineScope = rememberCoroutineScope()
    // CRITICAL FIX: neverEqualPolicy() forces Compose to redraw the UI every time the
    // database refreshes, preventing the UI from ignoring mutated database entities!
    var projects by remember { mutableStateOf<List<Project>>(emptyList(), neverEqualPolicy()) }
    var recentChats by remember { mutableStateOf<List<ChatSession>>(emptyList(), neverEqualPolicy()) }
    var expandedProjects by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var isProjectsSectionExpanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current // NEW: URI Handler
    var isOpeningPortal by remember { mutableStateOf(false) }

    // State for Delete All Confirmation Dialog
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var sessionToRename by remember { mutableStateOf<ChatSession?>(null) }
    var newSessionName by remember { mutableStateOf("") }

    // State for Project Dialogs
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showRenameProjectDialog by remember { mutableStateOf(false) }
    var showDeleteProjectDialog by remember { mutableStateOf(false) }
    var showSelectProjectDialog by remember { mutableStateOf(false) }
    var chatSessionsToMove by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    var projectToManage by remember { mutableStateOf<Project?>(null) }
    var projectNameField by remember { mutableStateOf("") }
    var projectInstructionsField by remember { mutableStateOf("") }

    var isThemeMenuExpanded by remember { mutableStateOf(false) }

    fun refreshChats() {
        coroutineScope.launch(Dispatchers.IO) {
            val updatedChats = aiService.getChatSessions()
            val updatedProjects = aiService.getProjects()
            withContext(Dispatchers.Main) {
                // .toList() forces a new memory reference, guaranteeing a UI redraw
                recentChats = updatedChats.toList()
                projects = updatedProjects.toList()
            }
        }
    }

    LaunchedEffect(refreshTrigger) {
        withContext(Dispatchers.IO) {
            refreshChats()
        }
    }

    // Local state for "Edit/Select" mode.
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedSessionIds by remember { mutableStateOf(setOf<Long>()) }
    var isStarredMenuOpen by remember { mutableStateOf(false) }
    var isRecentMenuOpen by remember { mutableStateOf(false) }

    fun exitSelectionMode() {
        isSelectionMode = false
        selectedSessionIds = emptySet()
    }
    val borderColor = NextGpuTheme.colors.border
    Column(
        modifier = Modifier
            .width(if (isCollapsed) SidebarCollapsedWidth else SidebarWidth) // THEME
            .fillMaxHeight()
            .background(NextGpuTheme.colors.backgroundVariant)
            // Custom draw modifier for a performant right-side border
            .drawWithContent {
                drawContent()
                val strokeWidth = BorderWidth.toPx()
                val x = size.width - strokeWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(SpacingMedium)
    ) {
        // --- Header Section ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isCollapsed) Arrangement.Center else Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingLarge)
                .height(HeightButtonCompact) // THEME: 32.dp
        ) {
            if (isCollapsed) {
                // Collapsed: Show Logo by default, swap to Expand Icon on hover
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()

                Box(
                    modifier = Modifier
                        .size(IconSizeStandard) // 24dp
                        .clip(RoundedCornerShape(RadiusSmall)) // Rounded corners on hover
                        .hoverable(interactionSource)
                        .background(
                            if (isHovered) NextGpuTheme.colors.hoverBackground else Color.Transparent
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true),
                            onClick = onToggleSidebar
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHovered) {
                        // Expand Icon on Hover
                        Icon(
                            painter = painterResource("icons/sidebar-right.svg"),
                            contentDescription = "Expand Sidebar",
                            tint = NextGpuTheme.colors.textSecondary,
                            modifier = Modifier.size(IconSizeSidebar) // 18dp
                        )
                    } else {
                        // Logo Default
                        val logoResource = "images/nextgpu-primary-logo.svg"


                        Icon(
                            painter = painterResource(logoResource),
                            contentDescription = "NextGPU Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(IconSizeStandard) // 24dp
                        )
                    }
                }
            } else {
                // Expanded: Full Logo + Toggle Button

                val logoResource = if (NextGpuTheme.colors.isDark) {
                    "images/nextgpu-secondary-logo-full.svg"
                } else {
                    "images/nextgpu-primary-logo-full.svg"
                }
                Icon(
                    painter = painterResource(logoResource),
                    contentDescription = "NextGPU Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.height(IconSizeStandard)
                )

                // Close Button (Using helper for consistent rounded hover)
                SidebarIconButton(
                    icon = "sidebar-left",
                    onClick = onToggleSidebar,
                    tint = NextGpuTheme.colors.textSecondary,
                )
            }
        }

        // --- Actions Section (New Chat, Search, Projects) ---
        val actionsAlpha = if (isSelectionMode) 0.5f else 1f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingMedium)
                .alpha(actionsAlpha)
        ) {
            SidebarItem(
                icon = "new-chat",
                label = "New Chat",
                isCollapsed = isCollapsed,
                onClick = { if(!isSelectionMode) { onNewChat() } },
            )
            // TODO: Implement global search
            SidebarItem(
                icon = "search", label = "Search Chats", isCollapsed = isCollapsed, onClick = {
                    if(!isSelectionMode) { /* TODO */ }
                }
            )

            // Project management section
            SidebarItem(
                icon = if (isProjectsSectionExpanded) "folder-empty" else "folder-open",
                label = "Projects",
                isCollapsed = isCollapsed,
                onClick = {
                    if (!isSelectionMode) {
                        isProjectsSectionExpanded = !isProjectsSectionExpanded
                    }
                }
            )

            AnimatedVisibility(
                visible = !isCollapsed && isProjectsSectionExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = SpacingMedium)) {
                    // Action to Create New Project
                    SidebarItem(
                        icon = "plus",
                        label = "New Project",
                        isCollapsed = false,
                        onClick = {
                            projectNameField = ""
                            projectInstructionsField = ""
                            showCreateProjectDialog = true
                        }
                    )

                    projects.forEach { project ->
                        ProjectItem(
                            project = project,
                            isExpanded = expandedProjects.contains(project.id),
                            onToggle = {
                                expandedProjects = if (expandedProjects.contains(project.id)) {
                                    expandedProjects - project.id
                                } else {
                                    expandedProjects + project.id
                                }
                            },
                            onRename = {
                                projectToManage = project
                                projectNameField = project.name ?: ""
                                projectInstructionsField = project.instructions ?: ""
                                showRenameProjectDialog = true
                            },
                            onDelete = {
                                projectToManage = project
                                showDeleteProjectDialog = true
                            },
                            isSelectionMode = isSelectionMode,
                            selectedSessionIds = selectedSessionIds,
                            onSessionClick = { session ->
                                if (isSelectionMode) {
                                    selectedSessionIds = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds - session.id
                                    } else {
                                        selectedSessionIds + session.id
                                    }
                                } else {
                                    onSessionSelected(session)
                                }
                            },
                            onAddToProject = { session ->
                                chatSessionsToMove = listOf(session)
                                showSelectProjectDialog = true
                            },
                            onRemoveFromProject = { session ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.removeChatSessionFromProject(session)
                                    withContext(Dispatchers.Main) {
                                        refreshChats()
                                    }
                                }
                            },
                            onDeleteSession = { session ->
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.deleteChatSession(session)
                                    withContext(Dispatchers.Main) {
                                        refreshChats()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- Chat History Section ---
        if (!isCollapsed) {
            Spacer(modifier = Modifier.height(SpacingMedium))

            // Selection Toolbar (Visible only in Selection Mode)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = SpacingSmall)
                        .padding(start = SpacingSmall) // Only start padding for alignment
                ) {
                    // Left: Select All Checkbox
                    val allSelected = selectedSessionIds.size == recentChats.size && recentChats.isNotEmpty()
                    CustomRoundedCheckbox(
                        checked = allSelected,
                        onCheckedChange = {
                            selectedSessionIds = if (allSelected) emptySet() else recentChats.map { it.id }.toSet()
                        },
                        modifier = Modifier.padding(end = SpacingSmall)
                    )

                    Text(
                        text = "${selectedSessionIds.size} selected",
                        style = MaterialTheme.typography.body2,
                        color = NextGpuTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Right: Actions (Move, Delete, Close)
                    val hasSelection = selectedSessionIds.isNotEmpty()

                    // Move to Project Action
                    SidebarIconButton(
                        icon = "folder-plus",
                        onClick = {
                            chatSessionsToMove = recentChats.filter { it.id in selectedSessionIds }
                            showSelectProjectDialog = true
                        },
                        enabled = hasSelection,
                        tint = if (hasSelection) NextGpuTheme.colors.textSecondary else NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    // Delete Selected Action
                    SidebarIconButton(
                        icon = "trash",
                        onClick = {
                            coroutineScope.launch(Dispatchers.IO) {
                                val sessionsToDelete = recentChats.filter { it.id in selectedSessionIds }
                                sessionsToDelete.forEach { aiService.deleteChatSession(it) }
                                withContext(Dispatchers.Main) {
                                    if (activeSessionId in selectedSessionIds) {
                                        onNewChat()
                                    }
                                    exitSelectionMode()
                                    refreshChats()
                                }
                            }
                        },
                        enabled = hasSelection,
                        tint = if (hasSelection) ErrorText else NextGpuTheme.colors.textSecondary.copy(alpha = 0.5f)
                    )

                    Spacer(modifier = Modifier.width(SpacingSmall))

                    // Close (Undo) Selection Mode Button
                    SidebarIconButton(
                        icon = "close",
                        onClick = { exitSelectionMode() },
                        tint = NextGpuTheme.colors.textSecondary,
                        size = IconSizeMedium,
                        iconSize = IconSizeMicro
                    )
                }
            }

            // TODO: Instead of two statements, do SpacingTiny x 2
            Spacer(modifier = Modifier.height(SpacingTiny))
            Spacer(modifier = Modifier.height(SpacingTiny))

            // ðŸš¨ CRITICAL FIX: Calculate lists OUTSIDE the LazyColumn so Compose
            // tracks the state read and structurally rebuilds the list on 0->1 transitions!
            val starredChats = recentChats.filter { it.starred == true }
            val unstarredChats = recentChats.filter { it.starred != true }

            // Chat List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SpacingMicro)
            ) {
                // --- STARRED SECTION ---
                if (starredChats.isNotEmpty()) {
                    // FIX: Added unique key for the header
                    item(key = "header_starred") {
                        ChatSectionHeader(
                            title = "Starred Chats",
                            isSelectionMode = isSelectionMode,
                            isMenuOpen = isStarredMenuOpen,
                            onMenuOpenChange = { isStarredMenuOpen = it },
                            onSelect = {
                                isSelectionMode = true
                                isStarredMenuOpen = false
                            },
                            onDeleteAll = {
                                showDeleteAllDialog = true
                                isStarredMenuOpen = false
                            }
                        )
                    }

                    itemsIndexed(
                        items = starredChats,
                        // FIX: Explicitly prefixing the key avoids Compose transition collisions
                        key = { _, session -> "star-${session.id ?: session.hashCode()}-${session.starred}" }
                    ) { _, session ->
                        val label = session.name ?: session.messages.firstOrNull { it.role == "user" }?.content?.take(20) ?: "Empty Chat"

                        ChatItem(
                            label = label,
                            isStarred = true,
                            isInProject = session.project != null,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedSessionIds.contains(session.id),
                            isActiveSession = session.id != null && session.id == activeSessionId,
                            onRename = {
                                sessionToRename = session
                                newSessionName = label.take(30)
                                showRenameDialog = true
                            },
                            onStar = {
                                // Optimistic Update: Instantly force the UI to react
                                recentChats = recentChats.map {
                                    if (it.id == session.id) it.apply { starred = false } else it
                                }
                                // Background Save
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.starChatSession(session, false)
                                    withContext(Dispatchers.Main) { refreshChats() }
                                }
                            },
                            onAddToProject = {
                                chatSessionsToMove = listOf(session)
                                showSelectProjectDialog = true
                            },
                            onRemoveFromProject = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.removeChatSessionFromProject(session)
                                    withContext(Dispatchers.Main) { refreshChats() }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.deleteChatSession(session)
                                    withContext(Dispatchers.Main) {
                                        refreshChats()
                                        if (session.id == activeSessionId) {
                                        onNewChat()
                                    }
                                    }
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedSessionIds = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds - session.id
                                    } else {
                                        selectedSessionIds + session.id
                                    }
                                } else {
                                    onSessionSelected(session)
                                }
                            }
                        )
                    }
                }

                // --- RECENT SECTION ---
                if (unstarredChats.isNotEmpty()) {
                    if (starredChats.isNotEmpty()) {
                        // FIX: Added unique key for the spacer
                        item(key = "spacer_recent") { Spacer(modifier = Modifier.height(SpacingSmall)) }
                    }

                    // FIX: Added unique key for the header
                    item(key = "header_recent") {
                        ChatSectionHeader(
                            title = "Recent Chats",
                            isSelectionMode = isSelectionMode,
                            isMenuOpen = isRecentMenuOpen,
                            onMenuOpenChange = { isRecentMenuOpen = it },
                            onSelect = {
                                isSelectionMode = true
                                isRecentMenuOpen = false
                            },
                            onDeleteAll = {
                                showDeleteAllDialog = true
                                isRecentMenuOpen = false
                            }
                        )
                    }

                    itemsIndexed(
                        items = unstarredChats,
                        // FIX: Explicitly prefixing the key avoids Compose transition collisions
                        key = { _, session -> "unstar-${session.id ?: session.hashCode()}-${session.starred}" }
                    ) { _, session ->
                        val label = session.name ?: session.messages.firstOrNull { it.role == "user" }?.content?.take(20) ?: "Empty Chat"

                        ChatItem(
                            label = label,
                            isStarred = false,
                            isInProject = session.project != null,
                            isSelectionMode = isSelectionMode,
                            isSelected = selectedSessionIds.contains(session.id),
                            isActiveSession = session.id != null && session.id == activeSessionId,
                            onRename = {
                                sessionToRename = session
                                newSessionName = label.take(30)
                                showRenameDialog = true
                            },
                            onStar = {
                                // Optimistic Update: Instantly force the UI to react
                                recentChats = recentChats.map {
                                    if (it.id == session.id) it.apply { starred = true } else it
                                }
                                // Background Save
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.starChatSession(session, true)
                                    withContext(Dispatchers.Main) { refreshChats() }
                                }
                            },
                            onAddToProject = {
                                chatSessionsToMove = listOf(session)
                                showSelectProjectDialog = true
                            },
                            onRemoveFromProject = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.removeChatSessionFromProject(session)
                                    withContext(Dispatchers.Main) { refreshChats() }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    aiService.deleteChatSession(session)
                                    withContext(Dispatchers.Main) {
                                        refreshChats()

                                        if (session.id == activeSessionId) {
                                            onNewChat()
                                        }
                                    }
                                }
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedSessionIds = if (selectedSessionIds.contains(session.id)) {
                                        selectedSessionIds - session.id
                                    } else {
                                        selectedSessionIds + session.id
                                    }
                                } else {
                                    onSessionSelected(session)
                                }
                            }
                        )
                    }
                }
            }
        } else {
            // Fills space when collapsed to push footer down
            Spacer(modifier = Modifier.weight(1f))
        }

        if (!isCollapsed) {
            Spacer(modifier = Modifier.height(SpacingMedium))
            ProviderPromoCard()
        }

        Spacer(modifier = Modifier.height(SpacingMedium))

        // --- Footer ---
        SidebarItem(icon = "help", label = "Help", isCollapsed = isCollapsed, onClick = { /* TODO: Open Help URL/Modal */ })
        var isThemeMenuExpanded by remember { mutableStateOf(false) }

        AnimatedVisibility(
            visible = viewModel.showOpenclawShortcut,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            CustomSidebarItem(
                iconPath = "images/openclaw-mascot.svg",
                label = if (isOpeningPortal) "Opening Portal..." else "OpenClaw Portal",
                isCollapsed = isCollapsed,
                // Custom OpenClaw Styling overrides
                iconTint = Color.Unspecified, // Keeps the mascot's native red color
                hoverBackgroundColor = OpenclawRed.copy(alpha = 0.15f),
                defaultTextColor = NextGpuTheme.colors.textPrimary,
                hoverTextColor = OpenclawRed,
                rippleColor = OpenclawRed,
                onClick = {
                    if (!isOpeningPortal) {
                        isOpeningPortal = true
                        coroutineScope.launch {
                            val url = viewModel.fetchOpenclawDashboardUrl()
                            uriHandler.openUri(url)
                            isOpeningPortal = false
                        }
                    }
                },
                // Example of how you'd use the trailing content later if you wanted an external link icon
                // trailingContent = {
                //     Icon(painterResource("icons/external-link.svg"), contentDescription = null, modifier = Modifier.size(12.dp), tint = NextGpuTheme.colors.textSecondary)
                // }
            )
        }

        SidebarItem(icon = "setting-alternate", label = "Settings", isCollapsed = isCollapsed, onClick = onSettings)

        // --- Delete All confirmation dialog ---
        if (showDeleteAllDialog) {
            Dialog(onDismissRequest = { showDeleteAllDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface, // Matches the setup dialogs
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog,
                            start = SpacingDialog,
                            end = SpacingDialog,
                            bottom = SpacingLarge // Tighter bottom padding to balance button height
                        )
                    ) {
                        // TITLE
                        Text(
                            text = "Delete all local chats?",
                            style = NextGpuTheme.typography.h6,
                            color = NextGpuTheme.colors.textPrimary, // Keeps your warning color
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        // BODY TEXT
                        Text(
                            text = "You are about to delete all chat history stored on your computer. This is your private data and not shared with anyone.\n\nThis operation cannot be undone. Are you sure?",
                            style = MaterialTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        // ACTION BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CANCEL BUTTON
                            CustomButton(
                                text = "Cancel",
                                onClick = { showDeleteAllDialog = false },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                hoverBackgroundColor = NextGpuTheme.colors.background.copy(0.35f),
                                elevation = false,

                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            // DESTRUCTIVE ACTION BUTTON
                            CustomButton(
                                text = "Yes, Delete All",
                                onClick = {
                                    showDeleteAllDialog = false
                                    coroutineScope.launch(Dispatchers.IO) {
                                        recentChats.forEach { recentChat -> aiService.deleteChatSession(recentChat) }
                                        withContext(Dispatchers.Main) {
                                            recentChats = emptyList()
                                            refreshChats()
                                            onNewChat()
                                        }
                                    }
                                },
                                backgroundColor = ErrorText, // Destructive red background
                                textColor = Primary01White,
                                elevation = false,

                            )
                        }
                    }
                }
            }
        }

        // --- Rename Chat Dialog ---
        if (showRenameDialog && sessionToRename != null) {
            RenameDialog(
                title = "Rename Chat",
                initialName = newSessionName,
                onDismiss = {
                    showRenameDialog = false
                    sessionToRename = null
                },
                onConfirm = { updatedName ->
                    val session = sessionToRename
                    if (session != null && updatedName.isNotBlank()) {
                        coroutineScope.launch(Dispatchers.IO) {
                            aiService.renameChatSession(session, updatedName.trim())
                            withContext(Dispatchers.Main) {
                                refreshChats()
                                showRenameDialog = false
                                sessionToRename = null
                            }
                        }
                    }
                }
            )
        }


        // --- Create Project Dialog ---
        if (showCreateProjectDialog) {
            ProjectDialog(
                title = "Create New Project",
                confirmLabel = "Create",
                name = projectNameField,
                onNameChange = { projectNameField = it },
                instructions = projectInstructionsField,
                onInstructionsChange = { projectInstructionsField = it },
                onConfirm = {
                    coroutineScope.launch(Dispatchers.IO) {
                        aiService.createProject(projectNameField.trim(), projectInstructionsField.trim())
                        withContext(Dispatchers.Main) {
                            refreshChats()
                            showCreateProjectDialog = false
                        }
                    }
                },
                onDismiss = { showCreateProjectDialog = false }
            )
        }

        // --- Rename Project Dialog ---
        if (showRenameProjectDialog && projectToManage != null) {
            ProjectDialog(
                title = "Update Project",
                confirmLabel = "Update",
                name = projectNameField,
                onNameChange = { projectNameField = it },
                instructions = projectInstructionsField,
                onInstructionsChange = { projectInstructionsField = it },
                onDismiss = { showRenameProjectDialog = false },
                onConfirm = {
                    val project = projectToManage
                    if (project != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            project.name = projectNameField.trim()
                            project.instructions = projectInstructionsField.trim()
                            aiService.updateProject(project)
                            withContext(Dispatchers.Main) {
                                refreshChats()
                                showRenameProjectDialog = false
                                projectToManage = null
                            }
                        }
                    }
                }
            )
        }

        // --- Delete Project Dialog ---
        if (showDeleteProjectDialog && projectToManage != null) {
            Dialog(onDismissRequest = { showDeleteProjectDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface, // Fixes the hardcoded white background!
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingExtraLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog,
                            start = SpacingDialog,
                            end = SpacingDialog,
                            bottom = SpacingLarge// Tighter bottom padding to balance button height
                        )
                    ) {
                        // TITLE
                        Text(
                            text = "Delete project?",
                            style = NextGpuTheme.typography.h6,
                            color = WarnText, // Retains the warning color for destructive actions
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        // BODY TEXT
                        Text(
                            text = "You are about to delete this project. It will NOT delete the chats inside it. However, custom instructions attached with this project will be lost.\n\nThis operation cannot be undone. Are you sure?",
                            style = MaterialTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
                        )

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        // ACTION BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CANCEL
                            CustomButton(
                                text = "Cancel",
                                onClick = { showDeleteProjectDialog = false },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                hoverBackgroundColor = NextGpuTheme.colors.background.copy(0.35f),
                                elevation = false,

                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            // DELETE (Destructive Action)
                            CustomButton(
                                text = "Delete",
                                onClick = {
                                    val project = projectToManage
                                    if (project != null) {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            aiService.deleteProject(project)
                                            withContext(Dispatchers.Main) {
                                                refreshChats()
                                                showDeleteProjectDialog = false
                                                projectToManage = null
                                            }
                                        }
                                    }
                                },
                                backgroundColor = ErrorText, // Red destructive background
                                textColor = Primary01White,
                                elevation = false, // Kept flat like your original design

                            )
                        }
                    }
                }
            }
        }

        // --- Select Project Dialog ---
        if (showSelectProjectDialog && chatSessionsToMove.isNotEmpty()) {
            Dialog(
                onDismissRequest = {
                    showSelectProjectDialog = false
                    chatSessionsToMove = emptyList()
                }
            ) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface, // Automatically handles Light/Dark mode
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = SpacingDialog,
                            start = SpacingDialog,
                            end = SpacingDialog,
                            bottom = SpacingLarge // Tighter bottom padding to balance button height
                        )
                    ) {
                        // TITLE
                        Text(
                            text = "Select Project",
                            style = NextGpuTheme.typography.h6,
                            color = NextGpuTheme.colors.textPrimary,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        // INSTRUCTION TEXT
                        Text(
                            text = "Choose a project to move the selected chat(s) to:",
                            style = MaterialTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary, // Themed color
                            modifier = Modifier.padding(bottom = SpacingMedium)
                        )

                        // SCROLLABLE PROJECT LIST
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp), // Keeps the list from growing infinitely
                            verticalArrangement = Arrangement.spacedBy(SpacingMicro)
                        ) {
                            itemsIndexed(projects) { _, project ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(RadiusSmall))
                                        .clickable {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                chatSessionsToMove.forEach { session ->
                                                    aiService.addChatSessionToProject(session, project)
                                                }
                                                withContext(Dispatchers.Main) {
                                                    exitSelectionMode()
                                                    refreshChats()
                                                    showSelectProjectDialog = false
                                                    chatSessionsToMove = emptyList()
                                                }
                                            }
                                        }
                                        .padding(SpacingSmall),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // ICON
                                    Icon(
                                        painter = painterResource("icons/collection.svg"),
                                        contentDescription = null,
                                        tint = NextGpuTheme.colors.textSecondary, // Themed icon tint
                                        modifier = Modifier.size(IconSizeMedium)
                                    )

                                    Spacer(modifier = Modifier.width(SpacingSmall))

                                    // PROJECT NAME
                                    Text(
                                        text = project.name ?: "Unnamed Project",
                                        style = MaterialTheme.typography.body2,
                                        color = NextGpuTheme.colors.textPrimary
                                    )
                                }
                            }

                            // EMPTY STATE
                            if (projects.isEmpty()) {
                                item {
                                    Text(
                                        text = "No projects found. Create a project first.",
                                        style = MaterialTheme.typography.body2,
                                        color = NextGpuTheme.colors.textSecondary,
                                        modifier = Modifier.padding(SpacingSmall)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(SpacingLarge))

                        // ACTION BUTTONS (Only Cancel needed here, as clicking a row confirms)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CustomButton(
                                text = "Cancel",
                                onClick = {
                                    showSelectProjectDialog = false
                                    chatSessionsToMove = emptyList()
                                },
                                backgroundColor = Color.Transparent,
                                hoverBackgroundColor = NextGpuTheme.colors.background.copy(0.35f),
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,

                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatSectionHeader(
    title: String,
    isSelectionMode: Boolean,
    isMenuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onSelect: () -> Unit,
    onDeleteAll: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightButtonCompact) // 32.dp
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = NextGpuTheme.colors.textSecondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (!isSelectionMode) {
            Box {
                SidebarIconButton(
                    icon = "dots-horizontal",
                    onClick = { onMenuOpenChange(true) },
                    tint = NextGpuTheme.colors.textSecondary,
                    iconSize = IconSizeSmall // 16dp
                )

                MaterialTheme(shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(RadiusMedium))) {
                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { onMenuOpenChange(false) },
                        modifier = Modifier
                            .background(NextGpuTheme.colors.background)
                            .border(BorderWidth, NextGpuTheme.colors.border, RoundedCornerShape(RadiusMedium))
                    ) {
                        Column(modifier = Modifier.padding(SpacingSmall)) {
                            StyledMenuItem(
                                icon = "checkbox",
                                text = "Select",
                                tint = Color.Unspecified,
                                onClick = onSelect
                            )
                            Divider(color = NextGpuTheme.colors.border, thickness = BorderWidth, modifier = Modifier.padding(vertical = SpacingTiny))
                            StyledMenuItem(
                                icon = "trash",
                                text = "Delete All",
                                isDestructive = true,
                                onClick = onDeleteAll
                            )
                        }
                    }
                }
            }
        }
    }
}