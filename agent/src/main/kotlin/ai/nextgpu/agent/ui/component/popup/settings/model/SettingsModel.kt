package ai.nextgpu.agent.ui.component.popup.settings.model

// ========================================================
// HIGHEST LEVEL: The Main Sidebar Tabs
// ========================================================
data class TabModel(
    val id: String,
    val label: String,
    val icon: String,
    val viewType: String, // e.g., "standard", "custom_hardware", "custom_models"
    val subTabs: List<SubTabModel> = emptyList()
)

// ========================================================
// SECOND LEVEL: The Top Sub-navigation (e.g., Appearance, Modes)
// ========================================================
data class SubTabModel(
    val id: String,
    val label: String,
    val sections: List<SettingsSectionModel>
)

// ========================================================
// THIRD LEVEL: A grouped section with a header
// ========================================================
data class SettingsSectionModel(
    val id: String,
    val title: String,
    val items: List<SettingItemModel>
)

// ========================================================
// FOURTH LEVEL: The actual interactive controls
// ========================================================
sealed class SettingItemModel {
    abstract val id: String
    abstract val title: String
    abstract val description: String?

    data class Toggle(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val isChecked: Boolean,
        val onCheckedChange: (Boolean) -> Unit
    ) : SettingItemModel()

    data class SingleChoice(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val options: List<String>,
        val selectedValue: String,
        val onOptionSelect: (String) -> Unit
    ) : SettingItemModel()

    data class Action(
        override val id: String,
        override val title: String,
        override val description: String? = null,
        val buttonText: String,
        val isDestructive: Boolean = false,
        val onClick: () -> Unit
    ) : SettingItemModel()
}
