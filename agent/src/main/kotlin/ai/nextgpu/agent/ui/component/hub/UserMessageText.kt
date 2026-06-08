package ai.nextgpu.agent.ui.component.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.material.Text
import androidx.compose.material.MaterialTheme
import ai.nextgpu.agent.ui.theme.NextGpuTheme
import ai.nextgpu.agent.ui.theme.PrimaryText01
import ai.nextgpu.agent.ui.theme.WarnText

@Composable
fun UserMessageText(
    content: String,
    searchText: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    // Memoize the styled string calculation to avoid re-parsing on every recomposition
    val textColor = NextGpuTheme.colors.textPrimary
    val styledText = remember(content, searchText, isCaseSensitive, isFocused) {
        if (searchText.isBlank()) {
            AnnotatedString(content)
        } else {
            buildAnnotatedString {
                val search = if (isCaseSensitive) searchText else searchText.lowercase()
                val source = if (isCaseSensitive) content else content.lowercase()
                var startIndex = 0

                while (true) {
                    val matchIndex = source.indexOf(search, startIndex)
                    if (matchIndex == -1) {
                        // Append remaining text
                        append(content.substring(startIndex))
                        break
                    }
                    // Append text before the match
                    append(content.substring(startIndex, matchIndex))

                    // Append the match with the highlight style

                    withStyle(
                        style = SpanStyle(
                            // Orange (Active) or Yellow (Inactive)
                            background = if (isFocused) WarnText else Color.Yellow,
                            color = textColor// Force black text for readability on highlight
                        )
                    ) {
                        append(content.substring(matchIndex, matchIndex + search.length))
                    }
                    startIndex = matchIndex + search.length
                }
            }
        }
    }

    Text(
        text = styledText,
        color = NextGpuTheme.colors.textPrimary,
        style = MaterialTheme.typography.body1,
        modifier = modifier
    )
}
