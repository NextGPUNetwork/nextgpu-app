package ai.nextgpu.agent.ui.component.popup.settings

// Core theme components

// Models and Config
import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.uniqueKey
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.common.CustomButton
import ai.nextgpu.agent.ui.component.popup.settings.components.StandardSettingsPage
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsSchemaConfig
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.component.popup.settings.model.SubTabModel
import ai.nextgpu.agent.ui.component.popup.settings.model.TabModel
import ai.nextgpu.agent.ui.component.popup.settings.modelmanagment.AvailableModelItem
import ai.nextgpu.agent.ui.component.popup.settings.modelmanagment.InstalledModelItem
import ai.nextgpu.agent.ui.component.popup.settings.tabs.OpenclawTabView
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.common.dto.AiModelDto
import ai.nextgpu.common.model.AiModelRegistry
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun SettingsPopup(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    currentTheme: AppThemeMode,
    onThemeChange: (AppThemeMode) -> Unit,
    isPrivateMode: Boolean,
    onPrivateModeChange: (Boolean) -> Unit,
    isAdvancedMode: Boolean,
    onAdvancedModeChange: (Boolean) -> Unit,
    onNavigateToNuke: () -> Unit,
    initialTabId: String = "general",
) {
    // 1. Load the dynamic configuration
    val tabs = remember(currentTheme, isPrivateMode, isAdvancedMode) {
        SettingsSchemaConfig.getTabs(
            currentTheme = currentTheme,
            onThemeChange = onThemeChange,
            isPrivateMode = isPrivateMode,
            onPrivateModeChange = onPrivateModeChange,
            isAdvancedMode = isAdvancedMode,
            onAdvancedModeChange = onAdvancedModeChange,
            onNavigateToNuke = onNavigateToNuke
        )
    }

    // Seed activeTabId from initialTabId, falling back to the first tab if not found
    var activeTabId by remember {
        mutableStateOf(
            if (initialTabId.isNotBlank() && tabs.any { it.id == initialTabId }) initialTabId
            else tabs.first().id
        )
    }

    // 3. Dynamically find the fresh tab object on every recomposition
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // Dimmed Background Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // The Main Modal Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f)
                .widthIn(max = MaxContentWidth)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(RadiusLarge),
            backgroundColor = NextGpuTheme.colors.background,
            elevation = ElevationExtraLarge
        ) {
            Row(modifier = Modifier.fillMaxSize()) {

                // ========================================================
                // LEFT SIDEBAR
                // ========================================================
                Column(
                    modifier = Modifier
                        .width(SidebarWidth)
                        .fillMaxHeight()
                        .background(NextGpuTheme.colors.surface)
                ) {
                    // Header
                    Box(
                        modifier = Modifier
                            .height(HeightTopBar)
                            .fillMaxWidth()
                            .padding(horizontal = SpacingLarge),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource("icons/setting-alternate.svg"),
                                contentDescription = null,
                                tint = NextGpuTheme.colors.textPrimary,
                                modifier = Modifier.size(IconSizeSmall)
                            )
                            Spacer(modifier = Modifier.width(SpacingMedium))
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.body1,
                                color = NextGpuTheme.colors.textPrimary
                            )
                        }
                    }

                    Divider(color = NextGpuTheme.colors.border)

                    // Tabs (Now iterating over the dynamic list)
                    Column(modifier = Modifier.padding(vertical = SpacingMedium)) {
                        tabs.forEach { tab ->
                            SettingsTabItem(
                                tab = tab,
                                isActive = activeTab.id == tab.id,
                                onClick = { activeTabId = tab.id }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))

                    if (viewModel.isUpdateAvailable) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(SpacingMedium),
                            contentAlignment = Alignment.Center
                        ) {
                            CustomButton(
                                text = viewModel.latestVersionInfo?.version?.let { "Update Available (v.$it)" } ?: "Update Available",
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { viewModel.setUpdatePopupVisibility(true) },
                                backgroundColor = NextGpuTheme.colors.primaryVariant.copy(alpha = 0.15f),
                                textColor = NextGpuTheme.colors.primaryVariant,
                                elevation = false,
                                contentPadding = PaddingValues(horizontal = SpacingMedium, vertical = 8.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(BorderWidth)
                        .fillMaxHeight()
                        .background(NextGpuTheme.colors.border)
                )

                // ========================================================
                // RIGHT CONTENT AREA
                // ========================================================
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .background(NextGpuTheme.colors.background)
                ) {
                    // Content Header
                    Row(
                        modifier = Modifier
                            .height(HeightTopBar)
                            .fillMaxWidth()
                            .padding(start = SpacingLarge, end = SpacingSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activeTab.label,
                            style = MaterialTheme.typography.h6,
                            color = NextGpuTheme.colors.textPrimary
                        )

                        // Close Button
                        Box(
                            modifier = Modifier
                                .size(HeightButtonCompact)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource("icons/close.svg"),
                                contentDescription = "Close",
                                tint = NextGpuTheme.colors.textSecondary,
                                modifier = Modifier.size(IconSizeMicro)
                            )
                        }
                    }

                    Divider(color = NextGpuTheme.colors.border)

                    // Scrollable Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()

                            .clipToBounds()
                    ) {
                        Crossfade(targetState = activeTabId) { tabId ->

                            val tabToRender = tabs.find { it.id == tabId } ?: tabs.first()

                            when (tabToRender.viewType) {
                                "standard" -> StandardTabView(subTabs = tabToRender.subTabs)
                                "custom_hardware" -> HardwareSettingsView()
                                "custom_models" -> ModelsSettingsView(viewModel)
                                "custom_openclaw" -> OpenclawTabView(viewModel)
                                else -> Text("Unknown Route", color = ErrorText)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// SUB-COMPONENTS
// ========================================================

@Composable
fun SettingsTabItem(
    tab: TabModel,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val isOpenclaw = tab.id == "openclaw"

    // 1. Determine the highlight color for the active indicator and background
    val highlightColor = if (isOpenclaw) OpenclawRed else NextGpuTheme.colors.primaryVariant

    // 2. Determine the text color
    val textColor = if (isActive) {
        if (isOpenclaw) OpenclawRed else NextGpuTheme.colors.textPrimary
    } else {
        NextGpuTheme.colors.textSecondary
    }

    // 3. Determine the icon tint (removing black/gray for Openclaw)
    val iconTint = if (isOpenclaw) {
        Color.Unspecified // Use Color.Unspecified if you want the native SVG colors to show instead
    } else {
        textColor
    }

    // 4. Apply the background color with opacity
    val backgroundColor = if (isActive) highlightColor.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .padding(horizontal = SpacingMedium, vertical = SpacingMicro)
            .fillMaxWidth()
            .height(HeightListItem)
            .clip(RoundedCornerShape(RadiusSmall))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(14.dp)
                    .background(highlightColor, RoundedCornerShape(SpacingMicro))
            )
            Spacer(modifier = Modifier.width(SpacingMedium))
        } else {
            Spacer(modifier = Modifier.width(3.dp + SpacingMedium))
        }

        Icon(
            painter = painterResource(tab.icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(IconSizeSmall)
        )

        Spacer(modifier = Modifier.width(SpacingMedium))

        Text(
            text = tab.label,
            style = MaterialTheme.typography.body1,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun StandardTabView(subTabs: List<SubTabModel>) {
    if (subTabs.isEmpty()) return

    var activeSubTabIndex by remember { mutableStateOf(0) }
    if (activeSubTabIndex >= subTabs.size) activeSubTabIndex = 0

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = SpacingLarge, vertical = SpacingSmall)) {
        SettingsSubNav(
            options = subTabs.map { it.label },
            selectedIndex = activeSubTabIndex,
            onOptionSelected = { activeSubTabIndex = it }
        )

        Spacer(modifier = Modifier.height(SpacingMedium))
        Divider(color = NextGpuTheme.colors.border)
        Spacer(modifier = Modifier.height(SpacingLarge))

        Crossfade(targetState = activeSubTabIndex) { index ->
            StandardSettingsPage(sections = subTabs[index].sections)
        }
    }
}

@Composable
fun SettingsSubNav(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightButtonStandard),
        horizontalArrangement = Arrangement.spacedBy(SpacingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex

            Box(
                modifier = Modifier
                    .height(HeightButtonCompact)
                    .clip(RoundedCornerShape(RadiusSmall))
                    .background(
                        if (isSelected) NextGpuTheme.colors.primaryVariant.copy(0.2f)
                        else Color.Transparent
                    )
                    .clickable { onOptionSelected(index) }
                    .padding(horizontal = SpacingLarge, vertical = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.button,
                    color = if (isSelected) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary
                )
            }
        }
    }
}

// --- Hardcoded Custom Views ---

@Composable
fun HardwareSettingsView() {
    Column {
        SettingsSectionHeader("GPU Configuration")
        Text("No GPU configuration available yet.", color = NextGpuTheme.colors.textSecondary)
    }
}

@Composable
fun ModelsSettingsView(viewModel: SettingsViewModel) {
    // App-lifetime singleton — survives popup close/reopen
    val downloadService = remember { springContext.getBean(ModelDownloadService::class.java) }

    // Only local UI state: search filter and expand toggles.
    var searchQuery by remember { mutableStateOf("") }
    var expandedModelNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    // NEW: Tab State
    var activeTabIndex by remember { mutableStateOf(0) }
    val tabOptions = listOf("All", "Text Generation", "Image Generation")

    // Dialog States
    var showModelDownloadStopDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<AiModelDto?>(null) }
    var modelToStop by remember { mutableStateOf<AiModelDto?>(null) }
    var showModelDeleteDialog by remember { mutableStateOf(false) }

    // NEW: List state to control scrolling behavior programmatically
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        downloadService.refresh()
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = SpacingLarge, vertical = SpacingSmall)) {
        // Header & Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = SpacingMedium)
                .height(HeightButtonCompact),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Model Management",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = NextGpuTheme.colors.textPrimary
            )

            BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .width(MaxContentWidth / 3)
                    .height(HeightButtonCompact)
                    .border(
                        width = BorderWidth,
                        color = NextGpuTheme.colors.border,
                        shape = RoundedCornerShape(RadiusExtraLarge)
                    )
                    .background(NextGpuTheme.colors.surface, RoundedCornerShape(RadiusExtraLarge)),
                singleLine = true,
                textStyle = MaterialTheme.typography.body2.copy(color = NextGpuTheme.colors.textPrimary),
                cursorBrush = SolidColor(NextGpuTheme.colors.textPrimary),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = SpacingMedium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NextGpuTheme.colors.textSecondary,
                            modifier = Modifier.size(IconSizeMedium)
                        )

                        Spacer(modifier = Modifier.width(SpacingSmall))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Filter Models",
                                    color = NextGpuTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.body2
                                )
                            }
                            innerTextField()
                        }
                    }
                }
            )
        }

        // Tag Filters
        SettingsSubNav(
            options = tabOptions,
            selectedIndex = activeTabIndex,
            onOptionSelected = { activeTabIndex = it }
        )

        Spacer(modifier = Modifier.height(SpacingMedium))

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = NextGpuTheme.colors.primaryVariant,
                    modifier = Modifier.size(IconSizeStandard)
                )
            }
        } else {
            // Filtering Logic (Search + Tags)
            val filteredModels = viewModel.availableModels.filter { model ->
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    model.model.contains(searchQuery, ignoreCase = true) ||
                            model.modelRegistry.contains(searchQuery, ignoreCase = true) ||
                            model.type.contains(searchQuery, ignoreCase = true)
                }

                val matchesTab = when (activeTabIndex) {
                    1 -> model.tasks?.contains("text-generation") == true
                    2 -> model.tasks?.contains("image-generation") == true
                    else -> true
                }

                matchesSearch && matchesTab
            }

            val uniqueModels = filteredModels.distinctBy { it.uniqueKey }

            // NEW: 3-Way Data Partitioning (Installing, Installed, Available)
            val installing = mutableListOf<AiModelDto>()
            val installed = mutableListOf<AiModelDto>()
            val available = mutableListOf<AiModelDto>()

            uniqueModels.forEach { model ->
                val isInstalled = if (model.modelRegistry == AiModelRegistry.OLLAMA.name) {
                    viewModel.installedModelNames.contains(model.model) ||
                            (model.model.indexOf(':') == -1 && viewModel.installedModelNames.contains("${model.model}:latest")) ||
                            viewModel.installedModelNames.any { it.startsWith("${model.model}:") } ||
                            (model.model.endsWith(":latest") && viewModel.installedModelNames.contains(model.model.substringBefore(":latest")))
                } else {
                    viewModel.installedModelNames.contains(model.model)
                }

                if (isInstalled) {
                    installed.add(model)
                } else {
                    val key = model.uniqueKey
                    val isActivelyInstalling = viewModel.downloadingModels.contains(key) ||
                            viewModel.pausedModels.contains(key) ||
                            viewModel.stoppingModels.contains(key)

                    if (isActivelyInstalling) {
                        installing.add(model)
                    } else {
                        available.add(model)
                    }
                }
            }

            // NEW: Auto-scroll effect. When a new item is added to the "installing" list, jump to top.
            val currentInstallingCount = installing.size
            var previousInstallingCount by remember { mutableStateOf(currentInstallingCount) }

            LaunchedEffect(currentInstallingCount) {
                if (currentInstallingCount > previousInstallingCount) {
                    listState.animateScrollToItem(0)
                }
                previousInstallingCount = currentInstallingCount
            }

            LazyColumn(
                state = listState, // Attach the state here to enable scrolling
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(SpacingSmall)
            ) {
                // Section 1: Installing
                if (installing.isNotEmpty()) {
                    item {
                        Text(
                            "Installing",
                            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold),
                            color = NextGpuTheme.colors.textPrimary, // Hinting it's an active process
                            modifier = Modifier.padding(bottom = SpacingSmall) // Remove top padding so it stays flush at top
                        )
                    }
                    items(installing, key = { it.uniqueKey }) { model ->
                        AvailableModelItem(
                            model = model,
                            isDownloading = viewModel.downloadingModels.contains(model.uniqueKey),
                            isPaused = viewModel.pausedModels.contains(model.uniqueKey),
                            isStopping = viewModel.stoppingModels.contains(model.uniqueKey),
                            downloadProgress = viewModel.downloadingProgress[model.uniqueKey] ?: 0.0,
                            isExpanded = expandedModelNames.contains(model.uniqueKey),
                            onToggleExpand = {
                                expandedModelNames = if (expandedModelNames.contains(model.uniqueKey)) {
                                    expandedModelNames - model.uniqueKey
                                } else {
                                    expandedModelNames + model.uniqueKey
                                }
                            },
                            onDownload = { viewModel.launchDownload(model) },
                            onPauseDownload = { viewModel.pauseDownload(model.uniqueKey) },
                            onResumeDownload = { viewModel.resumeDownload(model) },
                            onStopDownload = {
                                if (model.modelRegistry == AiModelRegistry.OLLAMA.name && viewModel.ollamaDownloadingCount > 1) {
                                    modelToStop = model
                                    showModelDownloadStopDialog = true
                                } else {
                                    viewModel.stopDownload(model)
                                }
                            }
                        )
                    }
                }

                // Section 2: Installed
                if (installed.isNotEmpty()) {
                    item {
                        Text(
                            "Installed",
                            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold),
                            color = NextGpuTheme.colors.textPrimary,
                            // Add top padding only if it's not the absolute first item in the list
                            modifier = Modifier.padding(
                                top = if (installing.isNotEmpty()) SpacingMedium else 0.dp,
                                bottom = SpacingSmall
                            )
                        )
                    }
                    items(installed, key = { it.uniqueKey }) { model ->
                        InstalledModelItem(
                            model = model,
                            isDeleting = viewModel.deletingModels.contains(model.uniqueKey),
                            isExpanded = expandedModelNames.contains(model.uniqueKey),
                            onToggleExpand = {
                                expandedModelNames = if (expandedModelNames.contains(model.uniqueKey)) {
                                    expandedModelNames - model.uniqueKey
                                } else {
                                    expandedModelNames + model.uniqueKey
                                }
                            },
                            onDelete = {
                                modelToDelete = model
                                showModelDeleteDialog = true
                            }
                        )
                    }
                }

                // Section 3: Available
                if (available.isNotEmpty()) {
                    item {
                        Text(
                            "Available",
                            style = MaterialTheme.typography.subtitle2.copy(fontWeight = FontWeight.Bold),
                            color = NextGpuTheme.colors.textPrimary,
                            modifier = Modifier.padding(
                                top = if (installing.isNotEmpty() || installed.isNotEmpty()) SpacingMedium else 0.dp,
                                bottom = SpacingSmall
                            )
                        )
                    }
                    items(available, key = { it.uniqueKey }) { model ->
                        AvailableModelItem(
                            model = model,
                            isDownloading = false, // Always false in this partition
                            isPaused = false,      // Always false in this partition
                            isStopping = false,    // Always false in this partition
                            downloadProgress = 0.0,
                            isExpanded = expandedModelNames.contains(model.uniqueKey),
                            onToggleExpand = {
                                expandedModelNames = if (expandedModelNames.contains(model.uniqueKey)) {
                                    expandedModelNames - model.uniqueKey
                                } else {
                                    expandedModelNames + model.uniqueKey
                                }
                            },
                            onDownload = { viewModel.launchDownload(model) },
                            onPauseDownload = { }, // Unreachable here
                            onResumeDownload = { },// Unreachable here
                            onStopDownload = { }   // Unreachable here
                        )
                    }
                }
            }
        }

        if (showModelDownloadStopDialog) {
            Dialog(onDismissRequest = { showModelDownloadStopDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier
                            // Apply uniform padding to top and sides
                            // Reduce bottom padding because buttons have their own internal height
                            .padding(
                                top = SpacingDialog,
                                start = SpacingDialog,
                                end = SpacingDialog,
                                bottom = SpacingLarge// Tighter bottom to balance the button height
                            )
                    ) {
                        Text(
                            text = "Stop Downloading Model",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "Stopping this model will cancel all other pending Ollama downloads currently in progress.",
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
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
                                    showModelDownloadStopDialog = false
                                    modelToStop = null
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                // Use zero vertical padding for the transparent button to align text baseline
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Stop",
                                onClick = {
                                    modelToStop?.let { viewModel.stopDownload(it) }

                                    // Reset state and close dialog
                                    showModelDownloadStopDialog = false
                                    modelToStop = null
                                },
                                backgroundColor = NextGpuTheme.colors.primary,
                                textColor = Primary03Black,
                                elevation = false,
                                // Keep the vertical padding here for the "pill" look
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }

        if (showModelDeleteDialog) {
            Dialog(onDismissRequest = { showModelDownloadStopDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(RadiusMedium),
                    color = NextGpuTheme.colors.surface,
                    contentColor = NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = SpacingLarge)
                ) {
                    Column(
                        modifier = Modifier
                            // Apply uniform padding to top and sides
                            // Reduce bottom padding because buttons have their own internal height
                            .padding(
                                top = SpacingDialog,
                                start = SpacingDialog,
                                end = SpacingDialog,
                                bottom = SpacingLarge// Tighter bottom to balance the button height
                            )
                    ) {
                        Text(
                            text = "Delete Model",
                            style = NextGpuTheme.typography.h6,
                            modifier = Modifier.padding(bottom = SpacingSmall)
                        )

                        Text(
                            text = "Are you sure to delete this model? You can download later again.",
                            style = NextGpuTheme.typography.body2,
                            color = NextGpuTheme.colors.textSecondary
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
                                    showModelDeleteDialog = false
                                    modelToDelete = null
                                },
                                backgroundColor = Color.Transparent,
                                textColor = NextGpuTheme.colors.textSecondary,
                                elevation = false,
                                // Use zero vertical padding for the transparent button to align text baseline
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )

                            Spacer(modifier = Modifier.width(SpacingSmall))

                            CustomButton(
                                text = "Delete",
                                onClick = {
                                    modelToDelete?.let { viewModel.deleteModel(it) }

                                    // Reset state and close dialog
                                    showModelDeleteDialog = false
                                    modelToDelete = null
                                },
                                backgroundColor = NextGpuTheme.colors.error,
                                textColor = Primary01White,
                                elevation = false,
                                // Keep the vertical padding here for the "pill" look
                                contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = SpacingSmall)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// SHARED EXPANDED DETAILS — extracted to avoid duplication
// ========================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModelExpandedDetails(model: AiModelDto) {
    Column(modifier = Modifier.padding(top = SpacingMedium)) {
        Divider(color = NextGpuTheme.colors.border)
        Spacer(modifier = Modifier.height(SpacingSmall))
        Text(
            text = model.description ?: "No description available.",
            style = MaterialTheme.typography.body2,
            color = NextGpuTheme.colors.textSecondary
        )
        if (model.tasks != null && model.tasks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(SpacingSmall))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SpacingTiny),
                verticalArrangement = Arrangement.spacedBy(SpacingTiny) // Adds spacing between wrapped lines
            ) {
                model.tasks.forEach { task ->
                    Box(
                        modifier = Modifier
                            .background(
                                NextGpuTheme.colors.primaryVariant.copy(alpha = 0.1f),
                                RoundedCornerShape(SpacingTiny)
                            )
                            .padding(horizontal = RadiusSmall, vertical = SpacingMicro)
                    ) {
                        Text(
                            text = task,
                            style = MaterialTheme.typography.overline,
                            color = NextGpuTheme.colors.primaryVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(text: String) {
    Column(modifier = Modifier.padding(bottom = SpacingMedium)) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Bold,
            color = NextGpuTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(SpacingTiny))
    }
}