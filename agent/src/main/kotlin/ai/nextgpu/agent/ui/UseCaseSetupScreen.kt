package ai.nextgpu.agent.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ai.nextgpu.agent.config.GlobalPropertyConfig
import ai.nextgpu.agent.service.NextGpuAgentService
import ai.nextgpu.agent.service.NextGpuAiService
import ai.nextgpu.agent.springContext
import ai.nextgpu.agent.ui.component.CustomButton
import ai.nextgpu.agent.ui.theme.*
import ai.nextgpu.agent.util.HardwareUtil
import kotlinx.coroutines.*

@Composable
fun UseCaseSetupScreen(
    onProceed: () -> Unit,
    onReturn: () -> Unit,
) {
    // NextGpuAgentService and NextGpuAiService from Spring context
    val service = remember { springContext.getBean(NextGpuAgentService::class.java) }
    val aiService = remember { springContext.getBean(NextGpuAiService::class.java) }
    val hwUtil = remember { springContext.getBean(HardwareUtil::class.java) }
    val scope = rememberCoroutineScope()
    var statusText by remember { mutableStateOf("Note! Each option requires downloading an AI model, which may take a while and consume additional storage.") }
    var isLoading by remember { mutableStateOf(false) }
    val models = remember { aiService.recommendOllamaModels(hwUtil.detectGpus().firstOrNull()) }
    val selected = remember { mutableStateListOf<Boolean>().apply { repeat(models.size) { add(false) } } }
    var downloadedModels by remember { mutableStateOf(setOf<String>()) }

    // Fetch already downloaded models on start
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                downloadedModels = aiService.listDownloadedModels().map { it.name }.toSet()
            } catch (e: Exception) {
                // Ignore or log error
            }
        }
    }

    val selectedModels = models.filterIndexed { index, _ -> selected[index] }
    val allSelectedDownloaded = selectedModels.isNotEmpty() && selectedModels.all { downloadedModels.contains(it.name) }
    val canLoad = selected.any { it } && !allSelectedDownloaded

    NextGpuTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NextGpuTheme.colors.background)
                .padding(SpacingLarge),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(SpacingMedium),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                // Same logo as WelcomeScreen
                val logoResource = if (NextGpuTheme.colors.isDark) {
                    "images/nextgpu-secondary-logo-full.svg"
                } else {
                    "images/nextgpu-primary-logo-full.svg"
                }
                Image(
                    painter = painterResource(logoResource),
                    contentDescription = "NextGPU Logo",
                    modifier = Modifier
                        .width(180.dp)
                        .padding(bottom = SpacingSmall),
                )

                // Same hero image as WelcomeScreen
                Image(
                    painter = painterResource("images/setup.svg"),
                    contentDescription = "Launch image",
                    modifier = Modifier.size(192.dp)
                )

                Text(
                    text = "What are you planning to use NextGPU for?",
                    style = MaterialTheme.typography.h5,
                    color = NextGpuTheme.colors.textPrimary,
                )

                // Selection and per-item download state
                val canLoadButton = selected.any { it }

                Column(
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    // Checklist rows
                    models.forEachIndexed { index, uc ->
                        val isDownloaded = downloadedModels.contains(uc.name)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = SpacingSmall)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selected[index],
                                    onCheckedChange = { checked -> selected[index] = checked },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Accent01Lime,
                                        uncheckedColor = NextGpuTheme.colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.width(SpacingSmall))
                                Text(
                                    text = if (!selected[index]) uc.useCase else {
                                        if (isDownloaded) "${uc.useCase} (Downloaded)"
                                        else "${uc.useCase} (loads ${uc.shortDescription}: ${uc.sizeInGB}GB)"
                                    },
                                    color = if (isDownloaded && selected[index]) NextGpuTheme.colors.textPrimary else NextGpuTheme.colors.textSecondary,
                                    style = MaterialTheme.typography.subtitle1
                                )
                            }
                            Spacer(modifier = Modifier.size(SpacingMedium))
                        }
                    }
                }
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.caption,
                    color = WarnText
                )

                CustomButton(
                    // Dynamically change text based on state
                    text = when {
                        isLoading -> "Loading..."
                        allSelectedDownloaded -> "Launch"
                        else -> "Load Models"
                    },
                    textColor = Primary03Black,
                    onClick = {
                        scope.launch {
                            if (allSelectedDownloaded) {
                                val usagePreferencesProperty = service.getGlobalProperty(GlobalPropertyConfig.USAGE_PREFERENCES)
                                usagePreferencesProperty.valueReference = "" // Clear previous if any
                                models.forEachIndexed { index, uc ->
                                    if (selected[index]) usagePreferencesProperty.valueReference += "${uc.name}, "
                                }
                                service.saveGlobalProperty(usagePreferencesProperty)
                                onProceed()
                            } else {
                                isLoading = true
                                try {
                                    val distro = service.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO).getValueReference()
                                    val username = service.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME).getValueReference()
                                    val password = service.getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD).getValueReference()

                                    // Iterate through selected models and deploy
                                    models.indices
                                        .filter { index -> selected[index] && !downloadedModels.contains(models[index].name) }
                                        .forEach { index ->
                                            val uc = models[index]
                                            try {
                                                statusText = "⏳ Loading model ${uc.name}..."
                                                val success = aiService.pullOllamaModel(uc.name)

                                                if (success) {
                                                    downloadedModels = downloadedModels + uc.name
                                                    logActivity("Successfully loaded model ${uc.name}")
                                                } else {
                                                    statusText = "Failed to load model ${uc.name}"
                                                }
                                            } catch (e: Exception) {
                                                val msg = e.message ?: "Model deployment failed"
                                                statusText = msg
                                                logActivity(msg)
                                            }
                                        }
                                    statusText = "All selected models are ready. You're good to go!"
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = canLoadButton && !isLoading,
                    backgroundColor = NextGpuTheme.colors.primary,
                )

                Spacer(modifier = Modifier.height(SpacingSmall))

                // The text-based skip button
                Text(
                    text = "Skip for now",
                    modifier = Modifier
                        .clickable(
                            onClick = { onProceed() }
                        )
                        .padding(SpacingSmall), // Adds a slightly larger touch target area
                    style = MaterialTheme.typography.button.copy(
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    ),
                    color = NextGpuTheme.colors.textSecondary
                )
            }
        }
    }
}
