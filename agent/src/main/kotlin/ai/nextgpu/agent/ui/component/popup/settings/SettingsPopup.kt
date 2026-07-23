package ai.nextgpu.agent.ui.component.popup.settings

// Core theme components

// Models and Config
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.component.popup.settings.components.StandardSettingsPage
import ai.nextgpu.agent.ui.component.popup.settings.model.*
import ai.nextgpu.agent.ui.component.popup.settings.tabs.OpenclawTabView
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
    // Load the dynamic configuration
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

    // Dynamically find the fresh tab object on every recomposition
    val activeTab = tabs.find { it.id == activeTabId } ?: tabs.first()

    // Dimmed Background Overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            },
        contentAlignment = Alignment.Center
    ) {
        // The Main Modal Card
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f)
                .widthIn(max = MaxContentWidth)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {})
                },
            shape = RoundedCornerShape(RadiusLarge + 5.dp),
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
                                painter = painterResource("icons/settings.svg"),
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
                                .clip(RoundedCornerShape(RadiusRound))
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
                                "custom_models" -> AiModelsSettingsView(viewModel)
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

    // Determine the highlight color for the active indicator and background
    val highlightColor = if (isOpenclaw) OpenclawRed else NextGpuTheme.colors.primaryVariant

    // Determine the text color
    val textColor = if (isActive) {
        if (isOpenclaw) OpenclawRed else NextGpuTheme.colors.textPrimary
    } else {
        NextGpuTheme.colors.textSecondary
    }

    // Determine the icon tint (removing black/gray for Openclaw)
    val iconTint = if (isOpenclaw) {
        Color.Unspecified // Use Color.Unspecified if you want the native SVG colors to show instead
    } else {
        textColor
    }

    // Apply the background color with opacity
    val backgroundColor = if (isActive) highlightColor.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .padding(horizontal = SpacingMedium, vertical = SpacingMicro)
            .fillMaxWidth()
            .height(HeightListItem)
            .clip(RoundedCornerShape(RadiusRound))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = SpacingMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
//        if (isActive) {
//            Box(
//                modifier = Modifier
//                    .width(3.dp)
//                    .height(14.dp)
//                    .background(highlightColor, RoundedCornerShape(SpacingMicro))
//            )
//            Spacer(modifier = Modifier.width(SpacingMedium))
//        } else {
//            Spacer(modifier = Modifier.width(3.dp + SpacingMedium))
//        }

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
                    .clip(RoundedCornerShape(RadiusRound))
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
        Column(modifier = Modifier.padding(bottom = SpacingMedium)) {
            Text(
                text = "GPU Configuration",
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.Bold,
                color = NextGpuTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(SpacingTiny))
        }
        Text("No GPU configuration available yet.", color = NextGpuTheme.colors.textSecondary)
    }
}