package ai.nextgpu.agent.ui.component.hub

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownHighlightedCodeFence
import com.mikepenz.markdown.m2.Markdown
import com.mikepenz.markdown.m2.markdownColor
import com.mikepenz.markdown.m2.markdownTypography
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxTheme
import ai.nextgpu.agent.ui.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.intellij.markdown.ast.getTextInNode

// Add these for the Markdown AST parsing
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

// Add these for Compose text styling (if you don't have them already)
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mikepenz.markdown.model.MarkdownTypography

// --- DATA STRUCTURES ---
private sealed class ContentBlock {
    data class MarkdownBlock(val content: String) : ContentBlock()
    data class LatexBlock(val latex: String) : ContentBlock()
    data class TableBlock(val rows: List<List<String>>, val headers: List<String>?) : ContentBlock()
}

// --- MAIN COMPOSABLE ---

@Composable
fun ResponseHandler(
    content: String,
    textColor: Color = PrimaryText01,
    isInsideTable: Boolean = false,
    searchText: String = "",
    isCaseSensitive: Boolean = false,
    isFocused: Boolean = false
) {
    // 1. Parse Content
    val blocks = remember(content) {
        parseContent(content)
    }

    // 2. SETUP SYNTAX HIGHLIGHTING
    // We grab the current theme colors.
    // Because 'ResponseHandler' recomposes when the theme changes, these values will be fresh.
    val isDark = NextGpuTheme.colors.isDark // Assuming this boolean exists in your theme
    val codeColor = NextGpuTheme.colors.textSecondary
    val commentColor = NextGpuTheme.colors.textSecondary
    val keywordColor = Primary02Purple
    val stringColor = CodeGreen
    val literalColor = WarnText
    val metaColor = InfoText

    // KEY FIX: We 'remember' the builder based on 'isDark' and the specific colors.
    // This ensures that when you switch modes, the SyntaxTheme is completely rebuilt
    // with the new color values (e.g., Black text instead of White).
    val highlightsBuilder = remember(isDark, codeColor, commentColor) {
        val customTheme = SyntaxTheme(
            key = "NextGpuTheme",
            code = codeColor.toArgb(),
            keyword = keywordColor.toArgb(),
            string = stringColor.toArgb(),
            literal = literalColor.toArgb(),
            comment = commentColor.toArgb(),
            metadata = metaColor.toArgb(),
            multilineComment = commentColor.toArgb(),
            punctuation = codeColor.toArgb(), // Punctuation now follows textPrimary (Black in light mode)
            mark = codeColor.toArgb()
        )

        Highlights.Builder()
            .theme(customTheme)
        // You can also add .language(...) here if you want to preload languages
    }

    // 3. Render Blocks
    Column(modifier = Modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.MarkdownBlock -> {
                    if (block.content.isNotBlank()) {
                        Markdown(
                            content = block.content,
                            colors = markdownColor(
                                codeBackground = NextGpuTheme.colors.backgroundVariant,
                                inlineCodeBackground = NextGpuTheme.colors.backgroundVariant,
                                dividerColor = NextGpuTheme.colors.border,
                                text = textColor
                            ),
                            components = markdownComponents(

                                codeBlock = { MarkdownHighlightedCodeBlock(it.content, it.node, highlights = highlightsBuilder) },
                                codeFence = {
                                    // LOGIC: If the content starts with "python" or "kotlin" due to parser quirks, use it.
                                    // Otherwise, we default to "" which renders as "CODE".
                                    val (language, cleanContent) = extractLanguageAndContent(it.content)

                                    CodeBlockWithHeader(
                                        language = language,
                                        content = cleanContent // <--- PASS CLEAN CODE HERE for the Copy Button
                                    ) {
                                        // Pass the 'highlightsBuilder' here as well
                                        MarkdownHighlightedCodeFence(
                                            content = it.content,
                                            node = it.node,
                                            highlights = highlightsBuilder
                                        )
                                    }
                                },
                                paragraph = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.body1.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                },
                                heading1 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h4.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                                    )
                                },
                                heading2 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h5.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                                    )
                                },
                                heading3 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h6.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                },
                                heading4 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h6.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                },
                                heading5 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h6.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                },
                                heading6 = {
                                    HighlightedMarkdownText(
                                        node = it.node, sourceText = it.content,
                                        typographyStyle = MaterialTheme.typography.h6.copy(color = textColor),
                                        typographyParams = markdownTypography(),
                                        searchText = searchText, isCaseSensitive = isCaseSensitive, isFocused = isFocused,
                                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                    )
                                }

                            ),
                            typography = markdownTypography(
                                h1 = MaterialTheme.typography.h4.copy(color = textColor),
                                h2 = MaterialTheme.typography.h5.copy(color = textColor),
                                h3 = MaterialTheme.typography.h6.copy(color = textColor),
                                h4 = MaterialTheme.typography.h6.copy(color = textColor),
                                h5 = MaterialTheme.typography.h6.copy(color = textColor),
                                h6 = MaterialTheme.typography.h6.copy(color = textColor),
                                paragraph = MaterialTheme.typography.body1.copy(color = textColor),
                                // 2. Style the text inside the highlight
                                code = MaterialTheme.typography.body2.copy(
                                    color = textColor,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium
                                ),
                                inlineCode = MaterialTheme.typography.body2.copy(
                                    color = Primary02Purple,
                                    fontFamily = JetBrainsMono,
                                    fontWeight = FontWeight.Medium
                                ),
                                quote = MaterialTheme.typography.body2.copy(
                                    color = NextGpuTheme.colors.textSecondary,
                                    fontStyle = FontStyle.Italic
                                ),
                                list = MaterialTheme.typography.body1.copy(color = textColor),
                                textLink = TextLinkStyles(
                                    style = MaterialTheme.typography.body1.copy(
                                        color = Primary02Purple,
                                        textDecoration = TextDecoration.Underline
                                    ).toSpanStyle()
                                )
                            )
                        )
                    }
                }
                is ContentBlock.LatexBlock -> {
                    // Render Math Image
                    LatexCodeBlock(
                        latex = block.latex,
                        textColor = textColor,
                        modifier = Modifier.padding(vertical = if (isInsideTable) 2.dp else 8.dp)
                    )
                }
                is ContentBlock.TableBlock -> {
                    // Render Grid Table
                    ComposableTable(
                        table = block,
                        textColor = textColor,
                        searchText = searchText,
                        isCaseSensitive = isCaseSensitive,
                        isFocused = isFocused
                    )
                }
            }
        }
    }
}

// --- TABLE RENDERER ---

@Composable
private fun ComposableTable(
    table: ContentBlock.TableBlock,
    textColor: Color,
    searchText: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val tableScrollState = rememberScrollState()

    // Logic to convert table data to CSV string for clipboard
    val csvData = remember(table) {
        val sb = StringBuilder()
        // Headers
        table.headers?.let { headers ->
            sb.append(headers.joinToString(",") { escapeCsv(it) })
            sb.append("\n")
        }
        // Rows
        table.rows.forEach { row ->
            sb.append(row.joinToString(",") { escapeCsv(it) })
            sb.append("\n")
        }
        sb.toString()
    }

    // Calculate how many columns this table has
    val colCount = remember(table) {
        table.headers?.size ?: table.rows.firstOrNull()?.size ?: 1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingMedium)
    ) {
        // Use BoxWithConstraints to calculate exactly how much horizontal space we have
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val bubbleWidth = maxWidth

            // 140.dp per column provides great readability without squishing text
            val minTableWidth = (colCount * 140).dp

            // If the table is wider than the bubble, force it to be wide and scroll.
            // If it's smaller, just let it fill the bubble naturally.
            val tableWidth = if (minTableWidth > bubbleWidth) minTableWidth else bubbleWidth

            // Wrapper to hold both the scrolling table AND the scrollbar
            Box(modifier = Modifier.fillMaxWidth()) {

                // The scrolling content
                Column(
                    modifier = Modifier
                        .horizontalScroll(tableScrollState)
                        .width(tableWidth) // STRICT WIDTH: Allows weight(1f) to work flawlessly
                        .padding(bottom = SpacingMedium) // Space for scrollbar
                ) {
                    // 1. Render Headers
                    table.headers?.let { headers ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = SpacingTiny)
                                .clip(RoundedCornerShape(RadiusSmall))
                                .background(NextGpuTheme.colors.backgroundVariant),
                        ) {
                            headers.forEach { header ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f) // Equal columns!
                                        .padding(
                                            horizontal = SpacingMedium,
                                            vertical = SpacingMedium
                                        )
                                ) {
                                    val cleanHeader = header.trim()
                                    val headerContent = if (cleanHeader.contains("**")) cleanHeader else "**$cleanHeader**"

                                    ResponseHandler(
                                        content = headerContent,
                                        textColor = textColor,
                                        isInsideTable = true,
                                        searchText = searchText,
                                        isCaseSensitive = isCaseSensitive,
                                        isFocused = isFocused
                                    )
                                }
                            }
                        }
                    }

                    // 2. Render Rows
                    table.rows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = SpacingMicro)
                        ) {
                            row.forEach { cellContent ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f) // Equal columns!
                                        .padding(
                                            horizontal = SpacingMedium,
                                            vertical = SpacingSmall
                                        ),
                                    contentAlignment = Alignment.TopStart
                                ) {
                                    ResponseHandler(
                                        content = cellContent.trim(),
                                        textColor = textColor,
                                        isInsideTable = true,
                                        searchText = searchText,
                                        isCaseSensitive = isCaseSensitive,
                                        isFocused = isFocused
                                    )
                                }
                            }
                            // Safety Check: If the AI generated a row with missing columns,
                            // add empty boxes so the row doesn't lose its alignment structure.
                            val missingCols = colCount - row.size
                            if (missingCols > 0) {
                                repeat(missingCols) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Horizontal scrollbar attached to the bottom of the table
                HorizontalScrollbar(
                    adapter = rememberScrollbarAdapter(tableScrollState),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = SpacingSmall),
                    style = ScrollbarStyle(
                        minimalHeight = 16.dp,
                        thickness = 6.dp,
                        shape = RoundedCornerShape(RadiusSmall),
                        hoverDurationMillis = 0,
                        unhoverColor = NextGpuTheme.colors.textSecondary.copy(0.4F),
                        hoverColor = NextGpuTheme.colors.textSecondary
                    )
                )
            }
        }

        // 3. Copy Footer (Stays fixed, matching bubble width)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = SpacingTiny)
                .clip(RoundedCornerShape(RadiusSmall))
                .background(NextGpuTheme.colors.backgroundVariant)
                .clickable {
                    clipboardManager.setText(AnnotatedString(csvData))
                }
                .padding(
                    horizontal = SpacingMedium,
                    vertical = SpacingSmall
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Copy table",
                style = MaterialTheme.typography.caption,
                color = NextGpuTheme.colors.textSecondary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = "Copy Table",
                tint = NextGpuTheme.colors.textSecondary,
                modifier = Modifier.size(IconSizeSmall)
            )
        }
    }
}

// Helper to handle CSV special characters (commas, newlines, quotes)
private fun escapeCsv(text: String): String {
    var cleanText = text
    if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
        cleanText = text.replace("\"", "\"\"") // Escape quotes
        return "\"$cleanText\"" // Wrap in quotes
    }
    return cleanText
}

// --- PARSING LOGIC (TOKENIZER) ---

private fun parseContent(text: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var cursor = 0
    val length = text.length

    while (cursor < length) {
        // 1. Scan for next Math Delimiter (ignoring escaped ones)
        val nextDollar = text.findValidDelimiter("$$", cursor)
        val nextBracket = text.findValidDelimiter("\\[", cursor)
        val nextParen   = text.findValidDelimiter("\\(", cursor)

        // 2. Scan for next Table Start
        // A table line must start with '|' (ignoring whitespace).
        // Usually, a table block starts on a new line.
        val nextTableRaw = text.indexOf("\n|", cursor)

        // Handle edge case: Table is at the very start of the string (index 0)
        val nextTableStart = if (cursor == 0 && text.startsWith("|")) 0
        else if (nextTableRaw != -1) nextTableRaw + 1
        else -1

        // 3. Determine Winner: Who comes first?
        val mathStarts = listOfNotNull(nextDollar, nextBracket, nextParen)
        val mathMatch = mathStarts.minByOrNull { it.first }

        val mathIdx = mathMatch?.first ?: Int.MAX_VALUE
        val tableIdx = if (nextTableStart != -1) nextTableStart else Int.MAX_VALUE

        // Case A: Nothing found -> Dump remaining text
        if (mathIdx == Int.MAX_VALUE && tableIdx == Int.MAX_VALUE) {
            blocks.add(ContentBlock.MarkdownBlock(text.substring(cursor)))
            break
        }

        if (tableIdx < mathIdx) {
            // Case B: Table Found First

            // Dump text before table
            if (tableIdx > cursor) {
                blocks.add(ContentBlock.MarkdownBlock(text.substring(cursor, tableIdx)))
            }

            // Extract the full table block
            val (tableEnd, tableBlock) = extractTableBlock(text, tableIdx)
            blocks.add(tableBlock)
            cursor = tableEnd

        } else {
            // Case C: Math Found First

            // Dump text before math
            if (mathIdx > cursor) {
                blocks.add(ContentBlock.MarkdownBlock(text.substring(cursor, mathIdx)))
            }

            // Extract Math
            val (startIndex, delimiterType) = mathMatch!!
            val closer = when (delimiterType) {
                "$$" -> "$$"
                "\\[" -> "\\]"
                "\\(" -> "\\)"
                else -> "$$"
            }

            val contentStart = startIndex + delimiterType.length
            val closerMatch = text.findValidDelimiter(closer, contentStart)

            if (closerMatch != null) {
                val (endIndex, _) = closerMatch
                val latexContent = text.substring(contentStart, endIndex).trim()
                blocks.add(ContentBlock.LatexBlock(latexContent))
                cursor = endIndex + closer.length
            } else {
                // Streaming Partial (No closer yet) -> Treat as text
                blocks.add(ContentBlock.MarkdownBlock(text.substring(startIndex)))
                break
            }
        }
    }

    return blocks
}

// --- HELPER FUNCTIONS ---

/**
 * Parses raw text starting at [startIndex] into a TableBlock.
 * Reads consecutive lines starting with '|'.
 */
private fun extractTableBlock(text: String, startIndex: Int): Pair<Int, ContentBlock.TableBlock> {
    var endIndex = startIndex
    val lines = mutableListOf<String>()

    var currentLineStart = startIndex
    while (currentLineStart < text.length) {
        val nextNewline = text.indexOf('\n', currentLineStart)
        val lineEnd = if (nextNewline == -1) text.length else nextNewline
        val line = text.substring(currentLineStart, lineEnd).trim()

        if (!line.startsWith("|")) {
            break // Table block ended
        }

        lines.add(line)
        endIndex = lineEnd + 1 // Move past newline
        currentLineStart = endIndex
    }

    // Parse Rows & Headers
    val rows = mutableListOf<List<String>>()
    var headers: List<String>? = null

    // Filter out separator lines like "| --- | --- |"
    val contentLines = lines.filter { !it.contains("---") }

    if (contentLines.isNotEmpty()) {
        // Assume first line is header
        headers = contentLines[0].trim('|').split("|").map { it.trim() }

        // Remaining lines are rows
        for (i in 1 until contentLines.size) {
            val cells = contentLines[i].trim('|').split("|").map { it.trim() }
            rows.add(cells)
        }
    }

    return endIndex to ContentBlock.TableBlock(rows, headers)
}

/**
 * Finds a delimiter, ignoring it if it is escaped by an ODD number of backslashes.
 * e.g. "\$$" is ignored, "\\$$" is valid.
 */
private fun String.findValidDelimiter(delimiter: String, startIndex: Int): Pair<Int, String>? {
    var index = this.indexOf(delimiter, startIndex)
    while (index != -1) {
        var backslashCount = 0
        var i = index - 1
        while (i >= 0 && this[i] == '\\') {
            backslashCount++
            i--
        }
        // Even backslashes (0, 2..) means NOT escaped. Odd means escaped.
        if (backslashCount % 2 == 0) {
            return index to delimiter
        }
        index = this.indexOf(delimiter, index + 1)
    }
    return null
}

@Composable
private fun CodeBlockWithHeader(
    language: String,
    content: String,
    codeBlockComponent: @Composable () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    // Normalize language (e.g., "kotlin" -> "KOTLIN")
    val languageLabel = remember(language) {
        if (language.isBlank()) "CODE" else language.capitalize()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = SpacingSmall) // 8.dp
            .clip(RoundedCornerShape(RadiusSmall)) // 8.dp -> 6.dp (Standardized)
        // .border(BorderWidth, StrokeGray, RoundedCornerShape(RadiusSmall))
    ) {
        // --- HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NextGpuTheme.colors.backgroundVariant)
                .padding(
                    horizontal = SpacingMedium, // 12.dp -> 10.dp
                    vertical = SpacingSmall     // 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = languageLabel,
                style = MaterialTheme.typography.caption,
                fontWeight = FontWeight.Medium,
                color = NextGpuTheme.colors.textSecondary,
                fontFamily = JetBrainsMono
            )

            // Copy Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(RadiusSmall)) // 4.dp -> 6.dp (Standardized)
                    .clickable { clipboardManager.setText(AnnotatedString(content)) }
                    .padding(SpacingTiny), // 4.dp
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = "Copy",
                    tint = NextGpuTheme.colors.textSecondary,
                    modifier = Modifier.size(IconSizeSmall) // 16.dp
                )
            }
        }

        // --- EXISTING HIGHLIGHTER COMPONENT ---
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            codeBlockComponent()
        }
    }
}

private fun extractLanguageAndContent(raw: String): Pair<String, String> {
    // 1. Find the Opening Fence (e.g. ```python)
    val startFenceIdx = raw.indexOf("```")
    if (startFenceIdx == -1) return "" to raw

    // Find where the first line ends (the newline after ```python)
    val endOfStartLine = raw.indexOf('\n', startFenceIdx)

    // If there is no newline, it's just a partial header like "```py"
    if (endOfStartLine == -1) {
        val language = raw.substring(startFenceIdx + 3).trim()
        return language to ""
    }

    // Extract Language
    val language = raw.substring(startFenceIdx + 3, endOfStartLine).trim()

    // 2. Find the Closing Fence (```) starting AFTER the first line
    // We look for the next occurrence of "```"
    val contentStart = endOfStartLine + 1
    val closingFenceIdx = raw.indexOf("```", contentStart)

    val cleanContent = if (closingFenceIdx != -1) {
        // CASE A: We found the closing fence.
        // Take ONLY the text between the start line and the closing fence.
        // This effectively chops off "#### 4. Matrix Mechanics..."
        raw.substring(contentStart, closingFenceIdx).trimEnd()
    } else {
        // CASE B: No closing fence yet (Still streaming).
        // Take everything we have so far.
        raw.substring(contentStart).trimEnd()
    }

    return language to cleanContent
}

@Composable
private fun HighlightedMarkdownText(
    node: ASTNode,
    sourceText: String,
    typographyStyle: androidx.compose.ui.text.TextStyle,
    typographyParams: MarkdownTypography,
    searchText: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean,
    modifier: Modifier = Modifier
) {
    val styledText = remember(node, sourceText, searchText, isCaseSensitive, isFocused, typographyStyle, typographyParams) {
        buildAnnotatedString {
            // 1. Build the markdown structure with the correct base style (e.g., H1, H2, or Paragraph)
            buildInlineMarkdown(node, sourceText, typographyParams)

            // 2. Inject highlights on top
            if (searchText.length >= 2) {
                applySearchHighlight(searchText, isCaseSensitive, isFocused)
            }
        }
    }

    Text(
        text = styledText,
        style = typographyStyle,
        modifier = modifier
    )
}

@Composable
private fun HighlightedParagraphText(
    rawText: String,
    searchText: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean,
    textColor: Color
) {
    val styledText = remember(rawText, searchText, isCaseSensitive, isFocused) {
        if (searchText.length < 2) {
            AnnotatedString(rawText)
        } else {
            buildAnnotatedString {
                val search = if (isCaseSensitive) searchText else searchText.lowercase()
                val source = if (isCaseSensitive) rawText else rawText.lowercase()
                var startIndex = 0

                while (true) {
                    val matchIndex = source.indexOf(search, startIndex)
                    if (matchIndex == -1) {
                        append(rawText.substring(startIndex))
                        break
                    }
                    append(rawText.substring(startIndex, matchIndex))

                    withStyle(
                        style = SpanStyle(
                            background = if (isFocused) WarnText else Color.Yellow,
                            color = Primary03Black,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(rawText.substring(matchIndex, matchIndex + search.length))
                    }
                    startIndex = matchIndex + search.length
                }
            }
        }
    }

    Text(
        text = styledText,
        style = MaterialTheme.typography.body1,
        color = textColor
    )
}

// 1. Recursively builds the text with native Markdown styling
// 1. Recursively builds the text with native Markdown styling
private fun AnnotatedString.Builder.buildInlineMarkdown(
    node: ASTNode,
    source: String,
    typography: MarkdownTypography
) {
    for (child in node.children) {
        when (child.type) {
            // Raw Text
            MarkdownTokenTypes.TEXT,
            MarkdownTokenTypes.SINGLE_QUOTE,
            MarkdownTokenTypes.DOUBLE_QUOTE -> {
                append(child.getTextInNode(source).toString())
            }
            MarkdownTokenTypes.WHITE_SPACE -> {
                append(" ")
            }

            // Markdown Elements
            MarkdownElementTypes.STRONG -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    buildInlineMarkdown(child, source, typography)
                }
            }
            MarkdownElementTypes.EMPH -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    buildInlineMarkdown(child, source, typography)
                }
            }

            // --- UPDATED BLOCK FOR INLINE CODE ---
            MarkdownElementTypes.CODE_SPAN -> {
                // Explicitly merge the typography with the background and purple color
                val codeStyle = typography.inlineCode.toSpanStyle().copy(
                    background = Secondary03LightGray,
                    color = Primary02Purple
                )
                withStyle(codeStyle) {
                    buildInlineMarkdown(child, source, typography)
                }
            }
            // -------------------------------------

            // Ignore the actual markdown syntax tokens so they don't print
            MarkdownTokenTypes.BACKTICK,
            MarkdownTokenTypes.EMPH -> { /* Do nothing */ }

            // Fallback for nested elements
            else -> {
                if (child.children.isNotEmpty()) {
                    buildInlineMarkdown(child, source, typography)
                }
            }
        }
    }
}

// 2. Applies highlight spans ON TOP of the existing styles
private fun AnnotatedString.Builder.applySearchHighlight(
    query: String,
    isCaseSensitive: Boolean,
    isFocused: Boolean
) {
    val fullText = this.toAnnotatedString().text
    val search = if (isCaseSensitive) query else query.lowercase()
    val source = if (isCaseSensitive) fullText else fullText.lowercase()

    var startIndex = 0
    while (true) {
        val matchIndex = source.indexOf(search, startIndex)
        if (matchIndex == -1) break

        // addStyle injects a span without overwriting the bold/italic spans underneath
        addStyle(
            style = SpanStyle(
                background = if (isFocused) WarnText else Color.Yellow,
                color = Primary03Black
            ),
            start = matchIndex,
            end = matchIndex + search.length
        )
        startIndex = matchIndex + search.length
    }
}
