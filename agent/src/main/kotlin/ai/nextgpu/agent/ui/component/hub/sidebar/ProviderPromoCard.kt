package ai.nextgpu.agent.ui.component.hub.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.nextgpu.agent.ui.component.common.CustomButton
import ai.nextgpu.agent.ui.component.common.IconPosition
import ai.nextgpu.agent.ui.theme.*

@Composable
fun ProviderPromoCard() {
    // We use a Box to layer the background decoration (bottom-left SVG) behind the content
    Card(
        elevation = ElevationNone,
        shape = RoundedCornerShape(RadiusMedium),
        backgroundColor = NextGpuTheme.colors.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1. DECORATION: SVG at Bottom Left
            Icon(
                painter = painterResource("images/promo-card-design.svg"),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.BottomEnd)

            )

            // 2. MAIN CONTENT
            Column(
                modifier = Modifier.padding(SpacingMedium)
            ) {
                Text(
                    text = "NextGPU Provider",
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold,
                    color = NextGpuTheme.colors.textPrimary
                )

                Spacer(modifier = Modifier.height(SpacingTiny))

                Text(
                    text = "Become a GPU provider today & start earning.",
                    style = MaterialTheme.typography.body2,
                    color = NextGpuTheme.colors.textPrimary,
                    lineHeight = 20.sp,
                    // LIMIT WIDTH: This forces the text to wrap once it exceeds 140dp
                    modifier = Modifier.widthIn(max = 160.dp)
                )

                Spacer(modifier = Modifier.height(SpacingLarge))

                // 3. CTA BUTTON (Aligned End)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    CustomButton(
                        text = "Coming Soon",
                        onClick = { /* TODO: Navigate */ },
//                        icon = painterResource("icons/arrow-right-no-line.svg"),
                        iconPosition = IconPosition.End,

                        // Custom styling for this specific instance
                        backgroundColor = Primary01White,
                        enabled = true,
                        textColor = Primary03Black,
                        contentPadding = PaddingValues(horizontal = SpacingLarge, vertical = 8.dp),

                    )
                }
            }
        }
    }
}
