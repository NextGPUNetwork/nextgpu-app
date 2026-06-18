package ai.nextgpu.agent.ui.component.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.service.NextGpuAiService
import ai.nextgpu.agent.ui.theme.*

@Composable
fun SuggestionCards(
    aiService: NextGpuAiService,
    onPromptSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(bottom = SpacingLarge), // THEME: 16.dp
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuggestionCard(
            text = "Image Generation",
            icon = "brush",
            onClick = {
                val template = aiService.getPromptTemplate("image-generation")
                onPromptSelected(template)
            }
        )
        Spacer(modifier = Modifier.width(SpacingMedium)) // THEME: 10.dp
        SuggestionCard(
            text = "Planning",
            icon = "gauge",
            onClick = {
                val template = aiService.getPromptTemplate("planning")
                onPromptSelected(template)
            }
        )
        Spacer(modifier = Modifier.width(SpacingMedium)) // THEME: 10.dp
        SuggestionCard(
            text = "Programming",
            icon = "ai-scan",
            onClick = {
                val template = aiService.getPromptTemplate("programming")
                onPromptSelected(template)
            }
        )
    }
}

@Composable
fun SuggestionCard(
    text: String,
    icon: String,
    onClick: () -> Unit
) {
    // THEME: Use RadiusRound (50.dp) for Pill shape
    val pillShape = RoundedCornerShape(RadiusRound)

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        color = Color.Transparent,
        shape = pillShape,
        elevation = ElevationNone,
        modifier = Modifier
            .clip(pillShape)
            // Use the hoisted interaction source for the container logic
            .hoverable(interactionSource)
            .background(
                color = if (isHovered) NextGpuTheme.colors.hoverBackground else Color.Transparent,
                shape = pillShape
            )
            .border(
                width = BorderWidth,
                color = if (isHovered) NextGpuTheme.colors.hoverBackground else NextGpuTheme.colors.border,
                shape = pillShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick
            )
    ) {
        // (Deleted the second inner interaction source from here)

        Row(
            modifier = Modifier
                .padding(horizontal = SpacingLarge, vertical = SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource("icons/$icon.svg"),
                contentDescription = icon,
                // Use the SAME hoisted 'isHovered' state
                tint = if (isHovered) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary,
                modifier = Modifier.size(IconSizeMedium)
            )

            Spacer(modifier = Modifier.width(SpacingSmall))

            Text(
                text = text,
                // Use the SAME hoisted 'isHovered' state
                color = if (isHovered) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary,
                style = MaterialTheme.typography.button,
            )
        }
    }
}
