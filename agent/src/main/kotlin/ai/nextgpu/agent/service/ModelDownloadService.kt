package ai.nextgpu.agent.service

import ai.nextgpu.common.dto.AiModelDto
import ai.nextgpu.common.model.AiModelRegistry
import ai.nextgpu.agent.model.PromptModel
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

// ============================================================
// PAUSE GATE
// ============================================================
class PauseGate {
    @Volatile var isPaused: Boolean = false
        private set

    fun pause() { isPaused = true }
    fun resume() { isPaused = false }
}

@Service
class ModelDownloadService @Autowired constructor(
    private val aiService: NextGpuAiService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _availableModels = mutableStateOf<List<AiModelDto>>(emptyList())
    var availableModels: List<AiModelDto>
        get() = _availableModels.value
        internal set(value) {
            _availableModels.value = value
        }

    // ----------------------------------------------------------------
    // installedModels — the SINGLE reactive source of truth for what is
    // installed. It is a Compose snapshot state (mutableStateOf), so any
    // composable that reads it (directly or through derivedStateOf) gets
    // recomposed automatically when refreshSync() rewrites it. This is what
    // replaces the old refreshVersion StateFlow + collectAsState bridge.
    // ----------------------------------------------------------------
    private val _installedModels = mutableStateOf<List<PromptModel>>(emptyList())
    var installedModels: List<PromptModel>
        get() = _installedModels.value
        internal set(value) {
            _installedModels.value = value
        }

    // Kept as its own backing field (set alongside installedModels) so the
    // Settings popup's fuzzy ":latest" matching keeps working unchanged and
    // without recomputing a Set on every read.
    private val _installedModelNames = mutableStateOf<Set<String>>(emptySet())
    var installedModelNames: Set<String>
        get() = _installedModelNames.value
        internal set(value) {
            _installedModelNames.value = value
        }

    private val _isLoading = mutableStateOf(false)
    var isLoading: Boolean
        get() = _isLoading.value
        internal set(value) {
            _isLoading.value = value
        }

    private val _downloadingModels = mutableStateOf<Set<String>>(emptySet())
    var downloadingModels: Set<String>
        get() = _downloadingModels.value
        internal set(value) {
            _downloadingModels.value = value
        }

    private val _ollamaDownloadingCount = mutableStateOf(0)
    var ollamaDownloadingCount: Int
        get() = _ollamaDownloadingCount.value
        internal set(value) {
            _ollamaDownloadingCount.value = value
        }

    private val _pausedModels = mutableStateOf<Set<String>>(emptySet())
    var pausedModels: Set<String>
        get() = _pausedModels.value
        internal set(value) {
            _pausedModels.value = value
        }

    private val _downloadingProgress = mutableStateOf<Map<String, Double>>(emptyMap())
    var downloadingProgress: Map<String, Double>
        get() = _downloadingProgress.value
        internal set(value) {
            _downloadingProgress.value = value
        }

    private val _deletingModels = mutableStateOf<Set<String>>(emptySet())
    var deletingModels: Set<String>
        get() = _deletingModels.value
        internal set(value) {
            _deletingModels.value = value
        }

    private val _stoppingModels = mutableStateOf<Set<String>>(emptySet())
    var stoppingModels: Set<String>
        get() = _stoppingModels.value
        internal set(value) {
            _stoppingModels.value = value
        }


    private val downloadingJobs = mutableMapOf<String, Job>()
    private val pauseGates = mutableMapOf<String, PauseGate>()
    private val stoppedModels = mutableSetOf<String>()

    fun refreshIfEmpty() {
        if (availableModels.isEmpty() && !isLoading) refresh()
    }

    fun refresh() {
        scope.launch {
            refreshSync()
        }
    }

    // ----------------------------------------------------------------
    // refreshSync — re-fetches the catalogue + installed list and writes
    // them into snapshot state. Dispatcher-safe: the blocking network/disk
    // calls are explicitly wrapped in Dispatchers.IO, so this is safe to
    // call from the Main thread (e.g. awaited from a LaunchedEffect) without
    // freezing the UI, as well as from the service's IO scope.
    // ----------------------------------------------------------------
    suspend fun refreshSync() {
        withContext(Dispatchers.Main) { isLoading = true }
        try {
            val models = withContext(Dispatchers.IO) { aiService.allAvailableModels }
            val installedRaw = withContext(Dispatchers.IO) { aiService.listDownloadedModels() }

            withContext(Dispatchers.Main) {
                val inFlightKeys = downloadingModels + pausedModels + stoppingModels

                val installed = installedRaw.filterNot { isInFlight(it.name, inFlightKeys) }

                availableModels = models
                installedModels = installed
                installedModelNames = installed.map { it.name }.toSet()
            }
        } catch (e: Exception) {
            // No longer silent — a failure here used to leave the UI stale
            // with no signal at all.
            e.printStackTrace()
        } finally {
            withContext(Dispatchers.Main) {
                isLoading = false
            }
        }
    }

    private fun isInFlight(installedName: String, inFlightKeys: Set<String>): Boolean {
        if (inFlightKeys.isEmpty()) return false
        return inFlightKeys.any { key ->
            installedName == key ||
                    installedName == "$key:latest" ||
                    installedName.startsWith("$key:") ||
                    (key.endsWith(":latest") && installedName == key.substringBefore(":latest"))
        }
    }

    // ----------------------------------------------------------------
    // Download
    // ----------------------------------------------------------------
    fun launchDownload(model: AiModelDto) {
        val uniqueKey = model.uniqueKey

        val gate = PauseGate()
        pauseGates[uniqueKey] = gate

        val job = scope.launch {
            withContext(Dispatchers.Main) {
                downloadingModels = downloadingModels + uniqueKey
                pausedModels = pausedModels - uniqueKey
            }
            stoppedModels.remove(uniqueKey)

            try {
                // No need for withContext(Dispatchers.IO) as the scope is already IO
                when (model.modelRegistry) {
                    AiModelRegistry.OLLAMA.name ->{
                        withContext(Dispatchers.Main) { ollamaDownloadingCount += 1 }
                        aiService.pullOllamaModel(
                            model.model,
                            { progress ->
                                scope.launch(Dispatchers.Main) { downloadingProgress = downloadingProgress + (uniqueKey to progress) }
                            },
                            {
                                if (stoppedModels.contains(uniqueKey)) return@pullOllamaModel true
                                if (pauseGates[uniqueKey]?.isPaused == true) return@pullOllamaModel true
                                false
                            }
                        )
                    }
                    AiModelRegistry.COMFY_UI.name -> aiService.downloadComfyUiModel(
                        model,
                        { progress ->
                            scope.launch(Dispatchers.Main) { downloadingProgress = downloadingProgress + (uniqueKey to progress) }
                        },
                        {
                            if (stoppedModels.contains(uniqueKey)) return@downloadComfyUiModel true
                            if (pauseGates[uniqueKey]?.isPaused == true) return@downloadComfyUiModel true
                            false
                        }
                    )
                    else -> throw IllegalArgumentException("Unsupported registry: ${model.modelRegistry}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (model.modelRegistry == AiModelRegistry.OLLAMA.name) {
                        ollamaDownloadingCount -= 1
                    }
                    when {
                        stoppedModels.contains(uniqueKey) -> { /* clear everything as today */ }
                        pauseGates[uniqueKey]?.isPaused == true -> {
                            pausedModels = pausedModels + uniqueKey
                            downloadingModels = downloadingModels - uniqueKey
                            // DO NOT clear downloadingProgress — keep last % for the UI
                        }
                        !isActive -> { pausedModels = pausedModels + uniqueKey; downloadingModels = downloadingModels - uniqueKey }
                        else -> { downloadingModels = downloadingModels - uniqueKey; downloadingProgress = downloadingProgress - uniqueKey }
                    }
                    downloadingJobs.remove(uniqueKey)
                    pauseGates.remove(uniqueKey)
                }

                // Rewrites installedModels → any composable reading it (HubScreen
                // via derivedStateOf, the Settings list directly) updates.
                refreshSync()
            }

        }

        downloadingJobs[uniqueKey] = job
    }

    // ----------------------------------------------------------------
    // Pause
    // ----------------------------------------------------------------
    fun pauseDownload(uniqueKey: String) {
        pauseGates[uniqueKey]?.pause()
        scope.launch(Dispatchers.Main) {
            pausedModels = pausedModels + uniqueKey
            downloadingModels = downloadingModels - uniqueKey
        }
    }

    // ----------------------------------------------------------------
    // Resume
    // ----------------------------------------------------------------
    fun resumeDownload(model: AiModelDto) {
        val uniqueKey = model.uniqueKey
        val gate = pauseGates[uniqueKey]
        if (gate != null && gate.isPaused) {
            gate.resume()
            scope.launch(Dispatchers.Main) {
                pausedModels = pausedModels - uniqueKey
                downloadingModels = downloadingModels + uniqueKey
            }
        } else {
            scope.launch(Dispatchers.Main) {
                pausedModels = pausedModels - uniqueKey
            }
            launchDownload(model)
        }
    }

    // ----------------------------------------------------------------
    // Stop
    // ----------------------------------------------------------------
    fun stopDownload(model: AiModelDto) {
        val uniqueKey = model.uniqueKey

        pauseGates[uniqueKey]?.resume()
        stoppedModels.add(uniqueKey)

        scope.launch(Dispatchers.Main) {
            stoppingModels = stoppingModels + uniqueKey
            pausedModels = pausedModels - uniqueKey
            downloadingModels = downloadingModels - uniqueKey
            downloadingProgress = downloadingProgress - uniqueKey
        }

        scope.launch {
            when (model.modelRegistry) {
                AiModelRegistry.OLLAMA.name -> aiService.deleteOllamaModel(model.model)
                AiModelRegistry.COMFY_UI.name -> aiService.deleteComfyUiModel(model)
                else -> false
            }
            withContext(Dispatchers.Main) {
                stoppedModels.remove(uniqueKey)
                stoppingModels = stoppingModels - uniqueKey
            }
        }
    }

    // ----------------------------------------------------------------
    // Delete installed model — refreshSync() at the end rewrites
    // installedModels, which HubScreen observes via derivedStateOf.
    // ----------------------------------------------------------------
    fun deleteModel(model: AiModelDto) {
        val uniqueKey = model.uniqueKey
        scope.launch {
            val hasActiveOllamaDownload = withContext(Dispatchers.Main) {
                deletingModels = deletingModels + uniqueKey
                ollamaDownloadingCount > 0
            }
            try {
                when (model.modelRegistry) {
                    AiModelRegistry.OLLAMA.name -> aiService.deleteOllamaModel(model.model, /* cleanupPartials = */ !hasActiveOllamaDownload)
                    AiModelRegistry.COMFY_UI.name -> aiService.deleteComfyUiModel(model)
                    else -> throw IllegalArgumentException("Unsupported registry: ${model.modelRegistry}")
                }
            } finally {
                withContext(Dispatchers.Main) { deletingModels = deletingModels - uniqueKey }
                refreshSync()
            }
        }
    }
}

val AiModelDto.uniqueKey: String get() = model