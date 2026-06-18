package ai.nextgpu.agent.ui.component.popup.settings

import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.RadiusSmall
import ai.nextgpu.agent.ui.theme.SpacingMedium
import ai.nextgpu.agent.ui.theme.SpacingMicro
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.agent.ui.theme.SpacingTiny
import ai.nextgpu.common.dto.AiModelDto
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// ========================================================
// SHARED EXPANDED DETAILS — extracted to avoid duplication
// ========================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiModelExpandedDetails(model: AiModelDto) {
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