package ai.nextgpu.agent.ui.component.popup.settings

import ai.nextgpu.agent.service.ModelDownloadService
import ai.nextgpu.agent.service.uniqueKey
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.ui.component.popup.settings.modelmanagment.AvailableModelItem
import ai.nextgpu.agent.ui.component.popup.settings.modelmanagment.InstalledModelItem
import ai.nextgpu.agent.ui.theme.BorderWidth
import ai.nextgpu.agent.ui.theme.HeightButtonCompact
import ai.nextgpu.agent.ui.theme.IconSizeMedium
import ai.nextgpu.agent.ui.theme.IconSizeStandard
import ai.nextgpu.agent.ui.theme.MaxContentWidth
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.Primary01White
import ai.nextgpu.agent.ui.theme.Primary03Black
import ai.nextgpu.agent.ui.theme.RadiusExtraLarge
import ai.nextgpu.agent.ui.theme.RadiusMedium
import ai.nextgpu.agent.ui.theme.SpacingDialog
import ai.nextgpu.agent.ui.theme.SpacingLarge
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.common.dto.AiModelDto
import ai.nextgpu.common.model.AiModelRegistry
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun AiModelsSettingsView(viewModel: SettingsViewModel) {
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
                    .background(
                        NextGpuTheme.colors.surface,
                        androidx.compose.foundation.shape.RoundedCornerShape(RadiusExtraLarge)
                    ),
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
                            (model.model.endsWith(":latest") && viewModel.installedModelNames.contains(
                                model.model.substringBefore(
                                    ":latest"
                                )
                            ))
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(RadiusMedium),
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(RadiusMedium),
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