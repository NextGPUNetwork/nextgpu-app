package ai.nextgpu.agent.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.ui.theme.*

@Composable
fun NumericStepper(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    min: Int,
    max: Int,
    step: Int = 128
) {
    // 1. Calculate limits for visual feedback
    val atMin = value <= min
    val atMax = value >= max

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = SpacingTiny)) {
        // LABEL
        Text(
            text = label,
            style = MaterialTheme.typography.caption, // Smaller label matching OutlinedTextField
            color = NextGpuTheme.colors.textSecondary, // Theme color fix!
            modifier = Modifier.padding(bottom = SpacingMicro)
        )

        // 2. Wrap the stepper in a bordered box so it visually matches the text fields
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.dp, NextGpuTheme.colors.border), // Theme border
                    RoundedCornerShape(RadiusSmall)
                )
                .padding(horizontal = SpacingMicro) // Internal padding
        ) {
            // DECREASE BUTTON
            IconButton(
                onClick = { if (!atMin) onChange(value - step) },
                enabled = !atMin, // 3. Actually disable the button when at the limit
                modifier = Modifier.size(SpacingHuge)
            ) {
                Icon(
                    painter = painterResource("icons/arrow-down.svg"),
                    contentDescription = "Decrease",
                    // Dim the icon if disabled, otherwise use primary text color
                    tint = if (atMin) NextGpuTheme.colors.textSecondary.copy(alpha = 0.3f) else NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.size(IconSizeMicro)
                )
            }

            // VALUE DISPLAY
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.body1,
                color = NextGpuTheme.colors.textPrimary, // Theme color fix!
                modifier = Modifier.weight(1f), // Pushes buttons to the edges
                textAlign = TextAlign.Center
            )

            // INCREASE BUTTON
            IconButton(
                onClick = { if (!atMax) onChange(value + step) },
                enabled = !atMax, // 3. Actually disable the button when at the limit
                modifier = Modifier.size(SpacingHuge)
            ) {
                Icon(
                    painter = painterResource("icons/arrow-up.svg"),
                    contentDescription = "Increase",
                    // Dim the icon if disabled, otherwise use primary text color
                    tint = if (atMax) NextGpuTheme.colors.textSecondary.copy(alpha = 0.3f) else NextGpuTheme.colors.textPrimary,
                    modifier = Modifier.size(IconSizeMicro)
                )
            }
        }
    }
}
