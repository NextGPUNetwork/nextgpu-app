package ai.nextgpu.agent.ui.component.hub

import ai.nextgpu.agent.springContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import org.springframework.core.env.Environment
import java.nio.file.Paths

// Assuming these are defined in your UI toolkit/theme files:
 import ai.nextgpu.agent.ui.theme.SpacingSmall
 import ai.nextgpu.agent.ui.theme.RadiusMedium
 import ai.nextgpu.agent.ui.theme.WarnText
import androidx.compose.foundation.shape.CircleShape
import kotlin.io.resolve
import kotlin.text.get

import ai.nextgpu.agent.ui.theme.IconSizeMicro
import ai.nextgpu.agent.ui.theme.IconSizeSmall
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.SpacingMicro
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import java.awt.Desktop

@Composable
fun ImageMessageContent(content: String) {
    val env = remember { springContext.getBean(Environment::class.java) }
    val filename = content.removePrefix("<IMG>image<IMG>:")
    val comfyBaseDir = env.getProperty("comfy.basedir")
    val outputDir = Paths.get(comfyBaseDir).resolve("output")
    val imagePath = outputDir.resolve(filename)

    val bitmap = remember(imagePath) {
        try {
            val file = imagePath.toFile()
            if (file.exists()) file.inputStream().buffered().use { loadImageBitmap(it) } else null
        } catch (e: Exception) { null }
    }

    if (bitmap != null) {
        Column(modifier = Modifier.padding(vertical = SpacingSmall)) {
            // Container for image with "Open Folder" button overlay
            Box(contentAlignment = Alignment.TopEnd) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Generated Image",
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .heightIn(max = 400.dp)
                        .clip(RoundedCornerShape(RadiusMedium))
                )

                // The "Open Folder" Icon
                Box(
                    modifier = Modifier
                        .padding(SpacingSmall)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(NextGpuTheme.colors.backgroundVariant)
                        .clickable {
                            try {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(outputDir.toFile())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource("icons/folder-empty.svg"), // Ensure you have this icon
                        contentDescription = "Open Output Folder",
                        tint = NextGpuTheme.colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        // We removed the filename display.
        // If the image fails, we show a subtle error instead of a path.
        Text("Unable to load generated image", color = WarnText, style = MaterialTheme.typography.caption)
    }
}