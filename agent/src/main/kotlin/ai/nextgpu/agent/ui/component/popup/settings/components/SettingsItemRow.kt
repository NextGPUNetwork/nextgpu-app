package ai.nextgpu.agent.ui.component.popup.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.SpacingLarge
import ai.nextgpu.agent.ui.theme.SpacingMedium

@Composable
fun SettingsItemRow(
    title: String,
    description: String?,
    controlContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = SpacingLarge)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                color = NextGpuTheme.colors.textPrimary,
                fontWeight = FontWeight.Medium
            )
            if (!description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    color = NextGpuTheme.colors.textSecondary
                )
            }
        }

        // The actual control (Switch, Dropdown, Button) goes here
        Box(modifier = Modifier.wrapContentWidth()) {
            controlContent()
        }
    }
}
