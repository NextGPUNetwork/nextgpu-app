package ai.nextgpu.agent.service

import ai.nextgpu.common.dto.VersionCheckDto
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

@Service
class VersionUpdateService @Autowired constructor(
    private val nextGpuWebService: NextGpuWebService,
    @Value("\${spring.application.version:0.1.0}") val currentVersion: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var currentTempFile: File? = null

    var isUpdateAvailable by mutableStateOf(false)
        internal set

    var latestVersionInfo by mutableStateOf<VersionCheckDto?>(null)
        internal set

    var isDownloading by mutableStateOf(false)
        internal set

    var downloadProgress by mutableStateOf(0f)
        internal set

    var updateError by mutableStateOf<String?>(null)
        internal set

    fun checkForUpdates() {
        scope.launch {
            try {
                val versionInfo = nextGpuWebService.latestVersion
                if (versionInfo != null && isNewerVersion(versionInfo.version, currentVersion)) {
                    withContext(Dispatchers.Main) {
                        latestVersionInfo = versionInfo
                        isUpdateAvailable = true
                    }
                }
            } catch (e: Exception) {
                // Check if it's a 404, which might mean the endpoint isn't implemented on server yet
                println("Failed to check for updates: ${e.message}")
            }
        }
    }

    /**
     * Compares two version strings to determine if the new version is newer than the current version.
     * Non-numeric version parts are ignored during the comparison.
     *
     * For example:
     * - isNewerVersion("1.2.beta", "1.2.0") compares "1.2" against "1.2.0",
     *   which is treated as equal, so the method returns false.
     *
     * - But it does not fully support semantic version strings like:
     * - 1.2.3-beta
     * - 1.2.3+build.5
     * - 1.2.3-SNAPSHOT
     *
     * @param newVersion The new version string to compare. Can be null.
     * @param currentVersion The current version string to compare against.
     * @return True if the new version is newer than the current version; false otherwise.
     */
    private fun isNewerVersion(newVersion: String?, currentVersion: String): Boolean {
        if (newVersion == null) return false
        val newParts = newVersion.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        val maxLength = maxOf(newParts.size, currentParts.size)
        for (i in 0 until maxLength) {
            val newVal = if (i < newParts.size) newParts[i] else 0
            val currVal = if (i < currentParts.size) currentParts[i] else 0
            if (newVal > currVal) return true
            if (newVal < currVal) return false
        }
        return false
    }

    fun startDownloadAndInstall() {
        val downloadUrl = latestVersionInfo?.downloadUrl ?: return
        if (isDownloading) return

        downloadJob = scope.launch {
            withContext(Dispatchers.Main) {
                isDownloading = true
                downloadProgress = 0f
                updateError = null
            }

            try {
                val version = latestVersionInfo?.version ?: currentVersion
                val tempFile = File(System.getProperty("java.io.tmpdir"), "NextGPU-$version.exe")
                currentTempFile = tempFile
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                downloadFile(downloadUrl, tempFile) { progress ->
                    downloadProgress = progress
                }

                withContext(Dispatchers.Main) {
                    launchInstallerAndExit(tempFile)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    // Reset state on cancellation
                    withContext(Dispatchers.Main) {
                        isDownloading = false
                        downloadProgress = 0f
                    }
                    currentTempFile?.delete()
                    currentTempFile = null
                    return@launch
                }
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    updateError = "Download failed: ${e.message}"
                }
            } finally {
                downloadJob = null
                currentTempFile = null
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        isDownloading = false

        // The file deletion is handled in the CancellationException block of the job
    }

    private suspend fun downloadFile(urlStr: String, destination: File, onProgress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw Exception("Server returned HTTP ${connection.responseCode}")
        }

        val fileLength = connection.contentLength
        connection.inputStream.use { input ->
            FileOutputStream(destination).use { output ->
                val data = ByteArray(8192)
                var total = 0L
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    yield() // Ensure cancellation check
                    total += count
                    if (fileLength > 0) {
                        onProgress(total.toFloat() / fileLength)
                    }
                    output.write(data, 0, count)
                }
            }
        }
    }

    private fun launchInstallerAndExit(installerFile: File) {
        try {
            // Inno Setup flags:
            // /SILENT - silent installation but show progress (and UAC prompt)
            // /SUPPRESSMSGBOXES - no message boxes
            // /CLOSEAPPLICATIONS - try to close the app if running
            // /RESTARTAPPLICATIONS - restart app after update (if it supports RegisterApplicationRestart)
            
            // Use 'cmd /c start' to launch the installer in a way that it can request UAC elevation properly
            // and remains detached from the current process.
            val command = listOf(
                "cmd.exe", "/c", "start", "\"NextGPU Installer\"",
                installerFile.absolutePath,
                "/SILENT",
                "/SUPPRESSMSGBOXES",
                "/CLOSEAPPLICATIONS",
                "/RESTARTAPPLICATIONS"
            )

            ProcessBuilder(command).start()

            // Give it a moment to actually start before we kill our own process
            Thread.sleep(1000)
            exitProcess(0)
        } catch (e: Exception) {
            updateError = "Failed to launch installer: ${e.message}"
            isDownloading = false
        }
    }
}
