package ai.nextgpu.agent.ui.component.popup.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.component.common.CustomButton
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingItemModel
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsSectionModel
import ai.nextgpu.agent.ui.theme.ErrorText
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.SpacingLarge
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingSmall

@Composable
fun StandardSettingsPage(
    sections: List<SettingsSectionModel>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = SpacingMedium),
        verticalArrangement = Arrangement.spacedBy(SpacingLarge)
    ) {
        sections.forEach { section ->
            Column {
                // Section Header
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.subtitle2,
                    color = NextGpuTheme.colors.textSecondary,
                    modifier = Modifier.padding(bottom = SpacingSmall)
                )

                // Render each item based on its type
                section.items.forEach { item ->
                    when (item) {
                        is SettingItemModel.Toggle -> {
                            SettingsToggle(
                                title = item.title,
                                description = item.description,
                                isChecked = item.isChecked,
                                onCheckedChange = item.onCheckedChange
                            )
                        }
                        is SettingItemModel.SingleChoice -> {
                            SettingsDropdown(
                                title = item.title,
                                description = item.description,
                                options = item.options,
                                selectedValue = item.selectedValue,
                                onOptionSelect = item.onOptionSelect
                            )
                        }
                        is SettingItemModel.Action -> {
                            // Wrapper for an action button
                            SettingsItemRow(title = item.title, description = item.description) {
                                CustomButton(
                                    text = item.buttonText,
                                    onClick = item.onClick,
                                    backgroundColor = if (item.isDestructive) ErrorText else NextGpuTheme.colors.surface,
                                    textColor = if (item.isDestructive) Color.White else NextGpuTheme.colors.textPrimary,
                                    contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = 8.dp),
                                )
                            }
                        }
                    }
                }

                Divider(
                    color = NextGpuTheme.colors.border,
                    modifier = Modifier.padding(top = SpacingMedium)
                )
            }
        }
    }
}
