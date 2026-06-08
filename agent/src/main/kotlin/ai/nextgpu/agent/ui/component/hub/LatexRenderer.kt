package ai.nextgpu.agent.ui.component.hub

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.SpacingSmall
import ai.nextgpu.agent.ui.theme.SpacingTiny
import org.scilab.forge.jlatexmath.TeXConstants
import org.scilab.forge.jlatexmath.TeXFormula
import org.scilab.forge.jlatexmath.TeXIcon
import java.awt.image.BufferedImage
import javax.swing.JLabel

/**
 * A Composable that renders LaTeX math equations as an image.
 */
@Composable
fun LatexCodeBlock(
    latex: String,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    // We remember the bitmap generation so it doesn't re-render on every frame/scroll
    val bitmap = remember(latex, textColor) {
        renderLatexToBitmap(latex, 20f, textColor)
    }

    Box(
        modifier = modifier
            .padding(vertical = SpacingTiny)
            .background(Color.Transparent),
        contentAlignment = Alignment.CenterStart
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Math Equation",
                modifier = Modifier.padding(start = SpacingTiny)
            )
        } else {
            // Fallback: If LaTeX is invalid (e.g. syntax error), just show the raw text
            Text(
                text = latex,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.caption
            )
        }
    }
}

/**
 * Pure helper function to convert LaTeX string -> AWT BufferedImage -> Compose ImageBitmap
 */
private fun renderLatexToBitmap(latex: String, textSize: Float, color: Color): ImageBitmap? {
    return try {
        val formula = TeXFormula(latex)

        // TeXConstants.STYLE_DISPLAY = 0 (Standard equation mode)
        val icon: TeXIcon = formula.createTeXIcon(0, textSize)

        // FIX: Use the Java setter directly
        icon.setForeground(java.awt.Color(color.toArgb()))

        val image = BufferedImage(icon.iconWidth, icon.iconHeight, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()

        val transparent = java.awt.Color(0, 0, 0, 0)
        g2.color = transparent
        g2.fillRect(0, 0, image.width, image.height)

        icon.paintIcon(JLabel(), g2, 0, 0)
        g2.dispose()

        image.toComposeImageBitmap()
    } catch (e: Exception) {
        null
    }
}
