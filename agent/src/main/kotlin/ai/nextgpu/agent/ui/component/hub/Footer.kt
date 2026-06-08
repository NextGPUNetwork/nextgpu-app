package ai.nextgpu.agent.ui.component.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.popup.settings.model.SettingsViewModel
import ai.nextgpu.agent.util.HardwareUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun Footer(viewModel: SettingsViewModel) {
    val hwUtil = remember { springContext.getBean(HardwareUtil::class.java) }
    var cpuPercent by remember { mutableStateOf(0) }
    var gpuPercent by remember { mutableStateOf(0) }
    var memoryPercent by remember { mutableStateOf(0) }
    var cpuTemp by remember { mutableStateOf(0) }
    var gpuTemp by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            withContext(Dispatchers.IO) {
                val cpu = hwUtil.readCurrentCpuUsage()
                val gpu = hwUtil.readCurrentGpuUsage()
                val mem = hwUtil.readCurrentMemoryUsage()
                val cpuT = hwUtil.readCurrentCpuTemperature()
                val gpuT = hwUtil.readCurrentGpuTemperature()

                withContext(Dispatchers.Main) {
                    cpuPercent = kotlin.math.round(cpu).toInt()
                    gpuPercent = gpu.toInt()
                    memoryPercent = mem.toInt()
                    cpuTemp = cpuT.toInt()
                    gpuTemp = gpuT.toInt()
                }
            }
            delay(1000)
        }
    }
    val borderColor = NextGpuTheme.colors.border
    Surface(
        color = NextGpuTheme.colors.backgroundVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(HeightFooter) // THEME: Replaced SpacingHuge with HeightFooter

            // Draw border line on top
            .drawWithContent {
                drawContent()
                val strokeWidth = BorderWidth.toPx()
                val y = strokeWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .clip(RoundedCornerShape(RadiusSmall)) // THEME: 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingSmall), // THEME: 8.dp
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "NextGPU v${viewModel.currentAppVersion}",
                color = NextGpuTheme.colors.textSecondary,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.weight(0.1f)
            )

            Text(
                text = "© 2025-2026 NextGPU. All rights reserved.",
                color =  NextGpuTheme.colors.textSecondary,
                style = MaterialTheme.typography.caption,
                modifier = Modifier.weight(0.4f)
            )

            Row(
                modifier = Modifier.weight(0.5f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall), // THEME: 8.dp
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CPU $cpuPercent%",
                        color = if (cpuPercent > 90) WarnText else NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "GPU $gpuPercent%",
                        color = if (gpuPercent > 90) WarnText else  NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = if (cpuTemp <= 0) "CPU Temp N/A" else "CPU Temp ${cpuTemp}°C",
                        color = if (cpuTemp > 90) WarnText else  NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "GPU Temp ${gpuTemp}°C",
                        color = if (gpuTemp > 90) WarnText else  NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption
                    )
                    Text(
                        text = "Memory $memoryPercent%",
                        color = if (memoryPercent > 90) WarnText else  NextGpuTheme.colors.textSecondary,
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}
