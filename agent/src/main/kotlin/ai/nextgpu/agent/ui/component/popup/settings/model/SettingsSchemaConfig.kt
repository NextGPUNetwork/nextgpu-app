package ai.nextgpu.agent.ui.component.popup.settings.model

import ai.nextgpu.agent.ui.component.popup.settings.model.SubTabModel
import ai.nextgpu.agent.ui.component.popup.settings.model.TabModel
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingItemModel
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsSectionModel
import ai.nextgpu.agent.ui.theme.AppThemeMode

object SettingsSchemaConfig {

    fun getTabs(
        currentTheme: AppThemeMode,
        onThemeChange: (AppThemeMode) -> Unit,
        isPrivateMode: Boolean,
        onPrivateModeChange: (Boolean) -> Unit,
        isAdvancedMode: Boolean,
        onAdvancedModeChange: (Boolean) -> Unit,
        onNavigateToNuke: () -> Unit
    ): List<TabModel> {
        return listOf(
            TabModel(
                id = "general",
                label = "General",
                icon = "icons/settings.svg",
                viewType = "standard",
                subTabs = listOf(
                    SubTabModel(
                        id = "general_appearance",
                        label = "Appearance",
                        sections = listOf(
                            SettingsSectionModel(
                                id = "section_appearance",
                                title = "Theme",
                                items = listOf(
                                    SettingItemModel.SingleChoice(
                                        id = "APP_THEME",
                                        title = "Application Theme",
                                        description = "Choose your preferred visual appearance for the application.",
                                        options = listOf("Light", "Dark", "System"),
                                        selectedValue = currentTheme.name,
                                        onOptionSelect = { selectedString ->
                                            onThemeChange(AppThemeMode.valueOf(selectedString))
                                        }
                                    )
                                )
                            )
                        )
                    ),
                    SubTabModel(
                        id = "general_modes",
                        label = "Application Modes",
                        sections = listOf(
                            SettingsSectionModel(
                                id = "section_modes",
                                title = "Application Modes",
                                items = listOf(
                                    SettingItemModel.Toggle(
                                        id = "IS_PRIVATE_MODE",
                                        title = "Private Mode",
                                        description = "Ensure no diagnostic data or prompt history is saved locally.",
                                        isChecked = isPrivateMode,
                                        onCheckedChange = onPrivateModeChange
                                    ),
//                                    SettingItemModel.Toggle(
//                                        id = "IS_ADVANCED_MODE",
//                                        title = "Advanced Mode",
//                                        description = "Unlock developer tools and granular LLM parameter tuning.",
//                                        isChecked = isAdvancedMode,
//                                        onCheckedChange = onAdvancedModeChange
//                                    )
                                )
                            )
                        )
                    ),
                    SubTabModel(
                        id = "general_data",
                        label = "Data",
                        sections = listOf(
                            SettingsSectionModel(
                                id = "section_data_management",
                                title = "Data Management",
                                items = listOf(
                                    SettingItemModel.Action(
                                        id = "ACTION_NUKE_DATA",
                                        title = "Nuke Data",
                                        description = "This will wipe all local data, chat history, and settings. This action cannot be undone.",
                                        buttonText = "Nuke Everything",
                                        isDestructive = true,
                                        onClick = onNavigateToNuke
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            TabModel(
                id = "models",
                label = "Models",
                icon = "icons/models.svg",
                viewType = "custom_models"
            ),
//            TabModel(
//                id = "hardware",
//                label = "Hardware",
//                icon = "icons/cpu.svg",
//                viewType = "custom_hardware"
//            ),
            TabModel(
                id = "openclaw",
                label = "OpenClaw",
                icon = "icons/openclaw.svg", // Update with your actual icon asset path
                viewType = "custom_openclaw"
            )
        )
    }
}
