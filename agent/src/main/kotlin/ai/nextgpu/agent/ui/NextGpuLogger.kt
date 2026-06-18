package ai.nextgpu.agent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight.Companion.SemiBold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import ai.nextgpu.agent.ui.theme.*
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// Enum class for log types
enum class LogType {
    INFO, WARNING, ERROR
}

// Log entry data class
data class LogEntry(
    val timestamp: String,
    val type: LogType,
    val message: String
)

// Object to hold the globally shared state
object NextGpuLogger {
    val logEntries = mutableStateListOf<LogEntry>()

    fun log(message: String, type: LogType = LogType.INFO) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        logEntries.add(LogEntry(timestamp, type, message))
    }

    fun clearLogs() {
        logEntries.clear()
    }

    fun saveLogsToFile(filePath: String): Boolean {
        return try {
            val content = logEntries.joinToString("\n") { entry ->
                "[${entry.timestamp}] [${entry.type}] ${entry.message}"
            }
            File(filePath).writeText(content)
            log("Logs saved to: $filePath")
            true
        } catch (e: Exception) {
            log("Failed to save logs: ${e.message}", LogType.ERROR)
            false
        }
    }
}


// Helper function to build the formatted log text with colors
@Composable
fun buildFormattedLogText(logs: List<LogEntry>): AnnotatedString {
    return buildAnnotatedString {
        logs.forEachIndexed { index, entry ->
            val logTypeColor = when (entry.type) {
                LogType.INFO -> InfoText
                LogType.WARNING -> WarnText
                LogType.ERROR -> ErrorText
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                append("[${entry.timestamp}] ")
            }
            withStyle(style = SpanStyle(color = logTypeColor)) {
                append("[${entry.type}] ")
            }
            withStyle(style = SpanStyle(color = MaterialTheme.colors.onSurface)) {
                append(entry.message)
            }
            if (index < logs.size - 1) {
                append("\n")
            }
        }
    }
}

// Helper function to use the logger anywhere in the app
private val logger = LoggerFactory.getLogger("ai.nextgpu.agent.ui.NextGpuLogger")

fun logActivity(message: String, type: LogType = LogType.INFO) {
    NextGpuLogger.log(message, type)
    when (type) {
        LogType.INFO -> logger.info(message)
        LogType.WARNING -> logger.warn(message)
        LogType.ERROR -> logger.error(message)
    }
}

/**
 * ActivityLogCard is a composable function that displays the log entries from NextGpuLogger
 * in a card format with options to save logs to a file and clear logs.
 */
@Composable
fun ActivityLogCard() {
    val scrollState = rememberScrollState()
    val logs by remember { derivedStateOf { NextGpuLogger.logEntries } }
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom when logs change
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = ElevationMedium, shape = RoundedCornerShape(RadiusMedium)),
        backgroundColor = Primary03Black,
        shape = RoundedCornerShape(RadiusMedium)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(SpacingLarge),
            verticalArrangement = Arrangement.spacedBy(SpacingMedium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Log",
                    style = MaterialTheme.typography.h6,
                    fontWeight = SemiBold,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(SpacingSmall)
                ) {
                    // Copy All button
                    IconButton(
                        onClick = {
                            val logText = logs.joinToString("\n") { entry ->
                                "[${entry.timestamp}] [${entry.type}] ${entry.message}"
                            }
                            clipboardManager.setText(AnnotatedString(logText))
                        },
                        modifier = Modifier.size(SpacingHuge),
                        enabled = logs.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy All Logs",
                            tint = if (logs.isEmpty()) PrimaryText02.copy(alpha = 0.5f) else PrimaryText02
                        )
                    }

                    // Save button
                    IconButton(
                        onClick = {
                            // In a real app, you'd use a file picker dialog
                            val homeDir = System.getProperty("user.home")
                            val fileName = "nextgpu_logs_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.txt"
                            val filePath = "$homeDir/$fileName"
                            NextGpuLogger.saveLogsToFile(filePath)
                        },
                        modifier = Modifier.size(SpacingHuge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Logs",
                            tint = PrimaryText02
                        )
                    }

                    // Clear button
                    IconButton(
                        onClick = { NextGpuLogger.clearLogs() },
                        modifier = Modifier.size(SpacingHuge)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear Logs",
                            tint = PrimaryText02
                        )
                    }
                }
            }

            Divider(
                color = Primary01White,
                thickness = ElevationMicro
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        color = Primary01White,
                        shape = RoundedCornerShape(RadiusSmall)
                    )
                    .padding(SpacingMedium)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activity logs yet. Actions will be recorded here.",
                            style = MaterialTheme.typography.body2,
                            color = PrimaryText02,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    SelectionContainer {
                        Text(
                            text = buildFormattedLogText(logs),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.body2,
                            color = Secondary03LightGray,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
        }
    }
}
