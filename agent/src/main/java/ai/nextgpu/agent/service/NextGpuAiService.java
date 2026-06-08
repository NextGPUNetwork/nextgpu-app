package ai.nextgpu.agent.service;

import ai.nextgpu.common.model.AiModelRegistry;
import ai.nextgpu.common.model.ComfyUiModelFile;
import ai.nextgpu.agent.util.OSUtil;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.model.*;
import ai.nextgpu.agent.repository.*;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.common.dto.GpuDto;
import ai.nextgpu.common.dto.AiModelDto;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.common.model.Gpu;

import ai.nextgpu.common.model.PosthogEvent;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * This service is to interact with the Ollama and other AI APIs.
 * It provides functionalities for generating responses, managing chat conversations,
 * and handling model lifecycle operations such as listing and pulling models.
 */
@Slf4j
@Getter
@Service
public class NextGpuAiService {

    //    @Autowired
    private final NextGpuWebService nextGpuWebService;

    private final RestTemplate restTemplate;
    private final RestTemplate longRunningRestTemplate;

    private final ObjectMapper objectMapper;

    private final String ollamaUrl;

    private final GlobalPropertyRepository globalPropertyRepository;

    private final ChatMessageRepository chatMessageRepository;

    private final ChatSessionRepository chatSessionRepository;

    private final ProjectRepository projectRepository;

    private final AnalyticsService analyticsService;

    private final String comfyBaseDir;

    private static int TOKEN_HISTORY_LIMIT = 4096;

    private int maxPinnedMessagesLimit = 10;
    @Autowired
    private HardwareUtil hardwareUtil;

    @Autowired
    public NextGpuAiService(NextGpuWebService nextGpuWebService, @Value("${ollama.api.url:http://localhost:${ollama.port:11434}}") String ollamaUrl, @Value("${comfy.basedir:./agent/comfy/basedir}") String comfyBaseDir, GlobalPropertyRepository globalPropertyRepository, ChatMessageRepository chatMessageRepository, ChatSessionRepository chatSessionRepository, ProjectRepository projectRepository, AnalyticsService analyticsService) {
        this.nextGpuWebService = nextGpuWebService;
        this.comfyBaseDir = comfyBaseDir;

        // Add timeouts so UI doesn't hang forever if the server is down/unreachable
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        rf.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        this.restTemplate = new RestTemplate(rf);

        // Dedicated RestTemplate for long-running operations like pulling models
        SimpleClientHttpRequestFactory longRunningRf = new SimpleClientHttpRequestFactory();
        longRunningRf.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        // Set a very high timeout for model downloads (e.g., 1 hour)
        longRunningRf.setReadTimeout((int) Duration.ofHours(1).toMillis());
        this.longRunningRestTemplate = new RestTemplate(longRunningRf);

        this.objectMapper = new ObjectMapper();
        this.ollamaUrl = ollamaUrl;
        this.globalPropertyRepository = globalPropertyRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.chatSessionRepository = chatSessionRepository;
        this.projectRepository = projectRepository;
        GlobalProperty prop = globalPropertyRepository.findByName(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT).orElse(null);
        if (prop != null) {
            TOKEN_HISTORY_LIMIT = prop.getCleanValue(prop.getValueReference());
        }

        GlobalProperty pinProp = globalPropertyRepository.findByName(GlobalPropertyConfig.MAX_PINNED_MESSAGES).orElse(null);
        if (pinProp != null) {
            try {
                // Parsing safely as Integer
                this.maxPinnedMessagesLimit = Integer.parseInt(pinProp.getValueReference());
            } catch (NumberFormatException e) {
                log.error("Invalid format for MAX_PINNED_MESSAGES. Using default: 10");
            }
        }

        this.analyticsService = analyticsService;
    }


    public List<AiModelDto> getAllAvailableModels() {
        return nextGpuWebService.getAllAvailableModels();
    }

    /**
     * Retrieves a list of AI models that are registered under the OLLAMA registry.
     *
     * @return a list of AiModelDto objects filtered by the OLLAMA model registry.
     */
    public List<AiModelDto> getOllamaModels() {
        return nextGpuWebService.getAllAvailableModels().stream().filter(m -> m.getModelRegistry().equals(AiModelRegistry.OLLAMA.name())).toList();
    }

    /**
     * Retrieves a list of AI models that belong to the ComfyUI registry.
     *
     * @return a list of AiModelDto objects filtered to include only those registered in the ComfyUI model registry.
     */
    public List<AiModelDto> getComfyUIModels() {
        return nextGpuWebService.getAllAvailableModels().stream().filter(m -> m.getModelRegistry().equals(AiModelRegistry.COMFY_UI.name())).toList();
    }

    public JsonNode generateResponseRaw(String modelName, String prompt, List<ChatMessage> messages, Float temperature) {
        try {
            List<PromptModel> promptModels = listDownloadedModels();
            boolean modelExists = promptModels.stream().anyMatch(m -> m.name.equals(modelName));
            if (!modelExists) {
                boolean pulled = pullOllamaModel(modelName);
                if (!pulled) {
                    ObjectNode err = objectMapper.createObjectNode();
                    err.put("error", "Failed to pull model '" + modelName + "'. If this is your first run, ensure Ollama is reachable at " + ollamaUrl + ".");
                    return err;
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("prompt", prompt);
            requestBody.set("messages", objectMapper.valueToTree(messages));
            requestBody.put("stream", false);

            if (temperature != null) {
                requestBody.put("temperature", temperature);
            }

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<JsonNode> response = restTemplate.postForEntity(ollamaUrl + "/api/generate", request, JsonNode.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("Exception generating completion", e);
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", getFriendlyConnectivityException(e));
            return err;
        }
    }

    /**
     * Generates a response based on the specified model, prompt, previous messages, and optional temperature setting.
     * This method ensures the specified model exists or attempts to pull it before generating the response.
     *
     * @param modelName   the name of the model to be used for generating the response
     * @param prompt      the prompt or query based on which the response is generated
     * @param messages    a list of previous chat messages to provide context for the response
     * @param temperature the optional temperature setting that controls the randomness of the response;
     *                    higher values make the output more random
     * @return the generated response as a String, or an error message if the generation process fails
     */
    public String generateResponse(String modelName, String prompt, List<ChatMessage> messages, Float temperature) {
        String availabilityProblem = checkAvailabilityProblem();
        if (availabilityProblem != null) {
            return availabilityProblem;
        }

        JsonNode jsonResponse = generateResponseRaw(modelName, prompt, messages, temperature);

        if (jsonResponse.has("error")) {
            return "Error: " + jsonResponse.get("error").asText();
        }

        if (jsonResponse.has("response")) {
            return jsonResponse.get("response").asText();
        } else {
            log.error("Unexpected response format: {}", jsonResponse);
            return "Error: Unexpected response format from Ollama";
        }
    }


    /**
     * Retrieves a list of downloaded models by making a request to the configured API endpoint.
     * The method parses the response and constructs a list of PromptModel objects representing the
     * models that have been downloaded. In case of an error or if the response is unsuccessful,
     * it returns an empty list.
     *
     * @return a list of PromptModel objects representing the downloaded models;
     * an empty list is returned if no models are found or an error occurs.
     */
    public List<PromptModel> listDownloadedModels() {
        List<PromptModel> allModels = new ArrayList<>();
        // 1. Get Ollama models
        try {
            ResponseEntity<JsonNode> response = restTemplate.getForEntity(ollamaUrl + "/api/tags", JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = response.getBody();
                if (root.has("models") && root.get("models").isArray()) {
                    for (JsonNode modelNode : root.get("models")) {
                        String name = modelNode.has("name") ? modelNode.get("name").asText() : "";
                        String modifiedAt = modelNode.has("modified_at") ? modelNode.get("modified_at").asText() : "";
                        String digest = modelNode.has("digest") ? modelNode.get("digest").asText() : "";
                        allModels.add(new PromptModel(name, modifiedAt, digest, AiModelRegistry.OLLAMA.name()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception listing Ollama models", e);
        }

        // 2. Get ComfyUI models
        try {
            List<AiModelDto> comfyModels = getComfyUIModels();
            if (comfyModels != null && !comfyModels.isEmpty()) {
                if (OSUtil.IS_WINDOWS) {
                    // In Windows, ComfyUI models are in WSL. We batch the existence check for efficiency.
                    StringBuilder checkCmd = new StringBuilder("set -eu; ");
                    Map<String, AiModelDto> modelMap = new HashMap<>();

                    for (AiModelDto model : comfyModels) {
                        List<ComfyUiModelFile> files = model.getFiles();
                        if (files == null || files.isEmpty()) continue;

                        modelMap.put(model.getModel(), model);
                        checkCmd.append("echo \"MODEL:").append(model.getModel()).append("\"; ");
                        for (ComfyUiModelFile file : files) {
                            String targetPath = comfyBaseDir + "/models/" + file.getTargetSubfolder() + "/" + file.getFileName();
                            String safePath = targetPath.replace("'", "'\"'\"'").replace('\\', '/');
                            checkCmd.append("if [ -f '").append(safePath).append("' ] && [ ! -f '").append(safePath)
                                    .append(".part' ]; then echo 'FOUND'; else echo 'MISSING'; fi; ");
                        }
                    }

                    if (!modelMap.isEmpty()) {
                        String distro = globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO).map(GlobalProperty::getValueReference).orElse("nextgpu");
                        String user = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME).map(GlobalProperty::getValueReference).orElse("nextgpu");
                        String password = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD).map(GlobalProperty::getValueReference).orElse("");

                        String output = password.isEmpty() ? OSUtil.executeCommandInWsl(checkCmd.toString(), distro, user) : OSUtil.executeCommandInWsl(checkCmd.toString(), distro, user, password);

                        log.debug("ComfyUI model existence check output: {}", output);

                        String[] lines = output.split("\n");
                        String currentModelName = null;
                        List<String> currentStatus = new ArrayList<>();

                        for (String line : lines) {
                            line = line.trim().replace("\0", "");
                            if (line.isEmpty()) continue;

                            if (line.startsWith("MODEL:")) {
                                if (currentModelName != null) {
                                    boolean allFound = !currentStatus.isEmpty() && currentStatus.stream().allMatch(s -> s.equals("FOUND"));
                                    if (allFound) {
                                        allModels.add(new PromptModel(currentModelName, null, null, AiModelRegistry.COMFY_UI.name()));
                                    }
                                }
                                currentModelName = line.substring(6);
                                currentStatus.clear();
                            } else if (line.equals("FOUND") || line.equals("MISSING")) {
                                currentStatus.add(line);
                            }
                        }
                        // Last one
                        if (currentModelName != null) {
                            boolean allFound = !currentStatus.isEmpty() && currentStatus.stream().allMatch(s -> s.equals("FOUND"));
                            if (allFound) {
                                allModels.add(new PromptModel(currentModelName, null, null, AiModelRegistry.COMFY_UI.name()));
                            }
                        }
                    }
                } else {
                    // Linux/macOS - local filesystem check
                    Path modelsDir = Paths.get(comfyBaseDir).resolve("models");
                    if (Files.exists(modelsDir)) {
                        for (AiModelDto model : comfyModels) {
                            boolean allFilesExist = true;
                            List<ComfyUiModelFile> files = model.getFiles();
                            if (files != null && !files.isEmpty()) {
                                for (ComfyUiModelFile file : files) {
                                    String targetSubfolder = file.getTargetSubfolder();
                                    String fileName = file.getFileName();
                                    if (targetSubfolder == null || fileName == null) {
                                        allFilesExist = false;
                                        break;
                                    }
                                    Path filePath = modelsDir.resolve(targetSubfolder).resolve(fileName);
                                    Path partPath = filePath.resolveSibling(fileName + ".part");
                                    if (!Files.exists(filePath) || Files.exists(partPath)) {
                                        allFilesExist = false;
                                        break;
                                    }
                                }
                            } else {
                                allFilesExist = false;
                            }

                            if (allFilesExist) {
                                allModels.add(new PromptModel(model.getModel(), null, null, AiModelRegistry.COMFY_UI.name()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception listing ComfyUI models", e);
        }

        return allModels;
    }

    /**
     * Pulls a model from the Ollama library
     */
    @Loggable
    public boolean pullOllamaModel(String modelName) {
        return pullOllamaModel(modelName, null);
    }

    /**
     * Pulls a model from the Ollama library with progress tracking and cancellation support
     *
     * @param modelName        the name of the model to pull
     * @param progressConsumer a consumer that receives progress updates (0.0 to 1.0)
     * @param cancelSupplier   a supplier that returns true if the pull should be cancelled
     * @return true if the model was successfully pulled, false otherwise
     */
    @Loggable
    public boolean pullOllamaModel(String modelName, Consumer<Double> progressConsumer, Supplier<Boolean> cancelSupplier) {
        try {
            String availabilityProblem = checkAvailabilityProblem();
            if (availabilityProblem != null) {
                log.error("Cannot pull model because Ollama is unavailable: {}", availabilityProblem);
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("name", modelName);
            if (progressConsumer == null) {
                requestBody.put("stream", false);
            } else {
                requestBody.put("stream", true);
            }

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            if (progressConsumer == null) {
                ResponseEntity<String> response = longRunningRestTemplate.postForEntity(ollamaUrl + "/api/pull", request, String.class);

                return response.getStatusCode().is2xxSuccessful();
            } else {
                return Boolean.TRUE.equals(longRunningRestTemplate.execute(ollamaUrl + "/api/pull", HttpMethod.POST, clientRequest -> {
                    clientRequest.getHeaders().addAll(headers);
                    clientRequest.getBody().write(objectMapper.writeValueAsBytes(requestBody));
                }, clientResponse -> {
                    if (clientResponse.getStatusCode().is2xxSuccessful()) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientResponse.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (cancelSupplier != null && Boolean.TRUE.equals(cancelSupplier.get())) {
                                    log.info("Pulling model {} was cancelled", modelName);
                                    return false;
                                }
                                try {
                                    JsonNode node = objectMapper.readTree(line);
                                    if (node.has("total") && node.has("completed")) {
                                        long total = node.get("total").asLong();
                                        long completed = node.get("completed").asLong();
                                        if (total > 0) {
                                            double progress = (double) completed / total;
                                            progressConsumer.accept(progress);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("Error parsing pull progress line: {}", line, e);
                                }
                            }
                        }
                        return true;
                    } else {
                        log.error("Error pulling model: {} - Status: {}", modelName, clientResponse.getStatusCode());
                        return false;
                    }
                }));
            }
        } catch (Exception e) {
            log.error("Error pulling model: {}", modelName, e);
            return false;
        }
    }

    /**
     * Pulls a model from the Ollama library with progress tracking
     *
     * @param modelName        the name of the model to pull
     * @param progressConsumer a consumer that receives progress updates (0.0 to 1.0)
     * @return true if the model was successfully pulled, false otherwise
     */
    @Loggable
    public boolean pullOllamaModel(String modelName, Consumer<Double> progressConsumer) {
        return pullOllamaModel(modelName, progressConsumer, null);
    }

    public boolean deleteOllamaModel(String modelName) {
        // Preserves existing behaviour (e.g. hard-stop) — still wipes partials.
        return deleteOllamaModel(modelName, true);
    }


    /**
     * Deletes a model from the Ollama library
     *
     * @param modelName the name of the model to delete
     * @return true if the model was successfully deleted, false otherwise
     */
    @Loggable
    public boolean deleteOllamaModel(String modelName, boolean cleanupPartials) {
        if (modelName == null || modelName.isEmpty()) {
            if (cleanupPartials) cleanupUnfinishedOllamaModels();
            return true;
        }
        try {
            String availabilityProblem = checkAvailabilityProblem();
            if (availabilityProblem != null) {
                log.error("Cannot delete model because Ollama is unavailable: {}", availabilityProblem);
                return false;
            }

            boolean exists = listDownloadedModels().stream().anyMatch(m -> m.getName().equalsIgnoreCase(modelName));
            String modelToDelete = modelName;

            if (!exists) {
                String alternativeName = modelName.contains(":") ? modelName : modelName + ":latest";
                if (!modelName.equalsIgnoreCase(alternativeName)) {
                    exists = listDownloadedModels().stream().anyMatch(m -> m.getName().equalsIgnoreCase(alternativeName));
                    if (exists) {
                        log.info("Model {} not found, but {} exists. Using it for deletion.", modelName, alternativeName);
                        modelToDelete = alternativeName;
                    }
                }
            }

            if (!exists) {
                log.warn("Model {} does not exist locally", modelName);
                if (cleanupPartials) cleanupUnfinishedOllamaModels();
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("name", modelToDelete);
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            ResponseEntity<String> response = restTemplate.exchange(ollamaUrl + "/api/delete", HttpMethod.DELETE, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to delete model {}. Status: {}", modelName, response.getStatusCode());
                if (cleanupPartials) cleanupUnfinishedOllamaModels();
                return false;
            }
            if (cleanupPartials) cleanupUnfinishedOllamaModels();
            return true;
        } catch (Exception e) {
            log.error("Error deleting model: {}", modelName, e);
            if (cleanupPartials) cleanupUnfinishedOllamaModels();
            return false;
        }
    }

    private void cleanupUnfinishedOllamaModels() {
        try {
            String partialsPath = "/usr/share/ollama/.ollama/models/blobs/*-partial*";
            String command = String.format("set -eu; " + "sudo systemctl stop ollama; " + "sudo rm -f %s; " + "sudo systemctl start ollama; " + "du -sh /usr/share/ollama/.ollama/models/", partialsPath);

            if (OSUtil.IS_WINDOWS) {
                String distro = globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO).map(GlobalProperty::getValueReference).orElse("nextgpu");
                String user = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME).map(GlobalProperty::getValueReference).orElse("nextgpu");
                String password = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD).map(GlobalProperty::getValueReference).orElse("");

                log.info("Cleaning up Ollama partials in WSL ({}): {}", distro, command);
                if (password.isEmpty()) {
                    OSUtil.executeCommandInWsl(command, distro, user);
                } else {
                    OSUtil.executeCommandInWsl(command, distro, user, password);
                }
            } else if (OSUtil.IS_LINUX) {
                log.info("Cleaning up Ollama partials in Linux: {}", command);
                ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
                Process process = pb.start();
                process.waitFor();
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup Ollama partials: {}", e.getMessage());
        }
    }

    /**
     * Downloads a ComfyUI model with progress tracking and cancellation support
     */
    @Loggable
    public boolean downloadComfyUiModel(AiModelDto model, Consumer<Double> progressConsumer, Supplier<Boolean> cancelSupplier) {

        if (model.getFiles() == null || model.getFiles().isEmpty()) {
            log.warn("No files found for ComfyUI model: {}", model.getModel());
            return true;
        }

        List<ComfyUiModelFile> files = model.getFiles();
        int totalFiles = files.size();

        safeProgress(progressConsumer, 0.0);

        for (int i = 0; i < totalFiles; i++) {

            if (isCancelled(cancelSupplier)) {
                log.info("Download cancelled: {}", model.getModel());
                return false;
            }

            ComfyUiModelFile file = files.get(i);
            final int completedFiles = i;

            boolean success = downloadComfyUiModelFile(file, fileProgress -> safeProgress(progressConsumer, (completedFiles + fileProgress) / (double) totalFiles), cancelSupplier);

            if (!success) {
                // Distinguish pause/stop (keep .part for resume) from a real failure (clean up).
                if (isCancelled(cancelSupplier)) {
                    log.info("Download paused/cancelled mid-file: {}", model.getModel());
                    return false;
                }
                log.error("Failed file {} for model {}", file.getFileName(), model.getModel());
                deleteComfyUiModel(model); // genuine failure — remove partials
                return false;
            }
        }

        safeProgress(progressConsumer, 1.0);
        return true;
    }

    private boolean downloadComfyUiModelFile(ComfyUiModelFile file, Consumer<Double> progressConsumer, Supplier<Boolean> cancelSupplier) {

        String downloadUrl = normalizeUrl(file.getUrl());
        String filename = file.getFileName();
        String modelType = file.getTargetSubfolder();

        if (OSUtil.IS_WINDOWS) {
            return downloadComfyUIModelInWsl(downloadUrl, filename, modelType, progressConsumer, cancelSupplier);
        }

        return downloadComfyUIModelViaHttp(downloadUrl, filename, modelType, progressConsumer, cancelSupplier);
    }

    private boolean downloadComfyUIModelInWsl(String downloadUrl, String filename, String modelType, Consumer<Double> progressConsumer, Supplier<Boolean> cancelSupplier) {

        String targetDir = comfyBaseDir + "/models/" + modelType;
        String targetPath = targetDir + "/" + filename;
        String partPath = targetPath + ".part";

        try {
            Files.createDirectories(Paths.get(targetDir));
            safeProgress(progressConsumer, 0.0);

            String cmd = "set -euo pipefail; " +
                    "mkdir -p '" + targetDir + "'; " +
                    "if [ -f '" + targetPath + "' ]; then echo 'EXISTS'; exit 0; fi; " +
                    "curl -fL -C - --progress-bar -o '" + partPath + "' '" + downloadUrl + "'"
                    + "&& mv -f '" + partPath + "' '" + targetPath + "'";

            String distro = globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO).map(GlobalProperty::getValueReference).orElse("nextgpu");

            String user = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME).map(GlobalProperty::getValueReference).orElse("nextgpu");

            String password = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD).map(GlobalProperty::getValueReference).orElse("");

            List<String> cmdList = List.of("wsl", "-u", user, "-d", distro, "--", "bash", "-c", password.isEmpty() ? cmd : "echo '" + password.replace("'", "'\"'\"'") + "' | sudo -S " + cmd);

            Process process = new ProcessBuilder(cmdList).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {

                    if (isCancelled(cancelSupplier)) {
                        process.destroyForcibly();
                        return false;
                    }

                    String trimmed = line.trim();

                    // ✅ CURL PROGRESS PARSE (THIS IS THE FIX)
                    if (trimmed.contains("%")) {
                        try {
                            String percent = trimmed.split("%")[0].replaceAll("[^0-9.]", "");
                            if (!percent.isEmpty()) {

                                double progress = Double.parseDouble(percent);

                                // IGNORE early 100% spikes
                                if (progress > 100.0) {
                                    progress = 99.0;
                                }

                                safeProgress(progressConsumer, progress / 100.0);
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    if (trimmed.contains("EXISTS")) {
                        safeProgress(progressConsumer, 1.0);
                        return true;
                    }
                }
            }

            int exit = process.waitFor();
            boolean success = exit == 0;

            if (success) {
                safeProgress(progressConsumer, 1.0);
            }

            return success;

        } catch (Exception e) {
            log.error("WSL download failed: {}", filename, e);
            return false;
        }
    }

    private boolean downloadComfyUIModelViaHttp(String downloadUrl, String filename, String modelType,
                                                Consumer<Double> progressConsumer, Supplier<Boolean> cancelSupplier) {
        Path targetDir = Paths.get(comfyBaseDir, "models", modelType);
        Path targetPath = targetDir.resolve(filename);
        Path partPath = targetDir.resolve(filename + ".part");

        try {
            Files.createDirectories(targetDir);
            if (Files.exists(targetPath)) { safeProgress(progressConsumer, 1.0); return true; }

            final long existing = Files.exists(partPath) ? Files.size(partPath) : 0L;

            // NOTE: do NOT delete partPath when !completed — it's needed to resume.
            return Boolean.TRUE.equals(longRunningRestTemplate.execute(
                    downloadUrl, HttpMethod.GET,
                    req -> { if (existing > 0) req.getHeaders().set(HttpHeaders.RANGE, "bytes=" + existing + "-"); },
                    response -> {
                        boolean append = response.getStatusCode().value() == 206; // server honored Range
                        long startFrom = append ? existing : 0L;
                        long remaining = response.getHeaders().getContentLength();
                        long totalSize = append ? startFrom + remaining : remaining;

                        if (!append) Files.deleteIfExists(partPath); // server ignored Range → clean restart

                        boolean cancelled = false;
                        try (InputStream is = response.getBody();
                             FileOutputStream fos = new FileOutputStream(partPath.toFile(), append)) {
                            byte[] buffer = new byte[8192];
                            long total = startFrom;
                            int read;
                            while ((read = is.read(buffer)) != -1) {
                                if (isCancelled(cancelSupplier)) { cancelled = true; break; }
                                fos.write(buffer, 0, read);
                                total += read;
                                if (totalSize > 0) safeProgress(progressConsumer, (double) total / totalSize);
                            }
                            fos.getFD().sync();
                        }
                        if (cancelled) return false;              // keep .part for resume
                        Files.move(partPath, targetPath, StandardCopyOption.ATOMIC_MOVE);
                        safeProgress(progressConsumer, 1.0);
                        return true;
                    }));

        } catch (Exception e) {
            log.error("HTTP download failed {}", filename, e);
            return false; // keep .part for resume
        }
    }

    private boolean isCancelled(Supplier<Boolean> cancelSupplier) {
        return cancelSupplier != null && Boolean.TRUE.equals(cancelSupplier.get());
    }

    private String normalizeUrl(String url) {
        return url != null && url.contains("/blob/") ? url.replace("/blob/", "/resolve/") : url;
    }

    private void safeProgress(Consumer<Double> consumer, double value) {
        if (consumer == null) return;
        consumer.accept(Math.clamp(value, 0.0, 1.0));
    }

    private void cleanupFile(String path, String distro, String user, String password) {
        try {
            String cmd = "rm -f '" + path + "'";
            if (password.isEmpty()) {
                OSUtil.executeCommandInWsl(cmd, distro, user);
            } else {
                OSUtil.executeCommandInWsl(cmd, distro, user, password);
            }
        } catch (Exception ignored) {
        }
    }


    /**
     * Deletes a ComfyUI model by removing its files
     */
    @Loggable
    public boolean deleteComfyUiModel(AiModelDto model) {
        if (model.getFiles() == null) return true;

        boolean allDeleted = true;
        for (ComfyUiModelFile file : model.getFiles()) {
            String filename = file.getFileName();
            String modelType = file.getTargetSubfolder();

            if (filename == null || modelType == null) continue;

            if (OSUtil.IS_WINDOWS) {
                String targetPath = comfyBaseDir + "/models/" + modelType + "/" + filename;
                try {
                    String safeTargetPath = targetPath.replace("'", "'\"'\"'").replace('\\', '/');
                    String cmd = "rm -f '" + safeTargetPath + "' '" + safeTargetPath + ".part'";

                    String distro = globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO).map(GlobalProperty::getValueReference).orElse("nextgpu");
                    String user = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME).map(GlobalProperty::getValueReference).orElse("nextgpu");
                    String password = globalPropertyRepository.findByName(GlobalPropertyConfig.OS_PASSWORD).map(GlobalProperty::getValueReference).orElse("");

                    if (password.isEmpty()) {
                        OSUtil.executeCommandInWsl(cmd, distro, user);
                    } else {
                        OSUtil.executeCommandInWsl(cmd, distro, user, password);
                    }
                } catch (Exception e) {
                    log.error("Failed to delete {} from WSL: {}", filename, e.getMessage());
                    allDeleted = false;
                }
            } else {
                Path filePath = Paths.get(comfyBaseDir).resolve("models").resolve(modelType).resolve(filename);
                try {
                    Files.deleteIfExists(filePath);
                    Files.deleteIfExists(filePath.resolveSibling(filename + ".part"));
                } catch (IOException e) {
                    log.error("Failed to delete {}: {}", filePath, e.getMessage());
                    allDeleted = false;
                }
            }
        }
        return allDeleted;
    }

    private String checkAvailabilityProblem() {
        try {
            restTemplate.getForEntity(ollamaUrl + "/api/tags", String.class);
            return null;
        } catch (ResourceAccessException e) {
            return "Error: Cannot reach AI service at " + ollamaUrl + ". " + "Reason: " + getFriendlyConnectivityException(e) + ". " + "On Windows, the services run in WSL, ensure the respective port is reachable from Windows.";
        } catch (Exception e) {
            return "Error: Ollama check failed at " + ollamaUrl + ": " + e.getMessage();
        }
    }

    private String getFriendlyConnectivityException(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            if (cur instanceof ConnectException) {
                return "connection refused (server not running or not reachable)";
            }
            if (cur instanceof SocketTimeoutException) {
                return "connection timed out (server slow/unreachable/firewall)";
            }
            cur = cur.getCause();
        }
        assert t != null;
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }

    /**
     * Recommends a list of generative AI models based on the GPU specifications provided.
     *
     * @param gpu the GPU instance containing details like model and VRAM to tailor the recommended generative AI models.
     *            If null, default recommendations are provided assuming an NVidia RTX 3060.
     * @return a list of {@code PromptModel} objects, each representing a use case
     * with corresponding recommended AI model, memory requirements, and description.
     */
    @Loggable
    public List<PromptModel> recommendOllamaModels(Gpu gpu) {
        List<PromptModel> list = new ArrayList<>();
        // Try to detect GPU if null
        if (gpu == null) {
            Set<Gpu> gpus = hardwareUtil.detectGpus();
            gpu = gpus.stream().filter(g -> g.getManufacturer() != null && g.getManufacturer().equalsIgnoreCase("nvidia")).findFirst().orElseGet(() -> gpus.stream().findFirst().orElse(null));
        }
        if (gpu != null) {
            GpuDto dto = new GpuDto();
            BeanUtils.copyProperties(gpu, dto);
            try {
                List<AiModelDto> models = nextGpuWebService.getAvailableModelsByGpu(dto);
                if (!models.isEmpty()) {
                    models.stream().filter(model -> Objects.equals(model.getType(), "general")).findFirst().ifPresent(generalModel -> list.add(new PromptModel(generalModel.getModel(), null, null, "Creative Writing/General Reasoning", generalModel.getSizeInGB().floatValue(), generalModel.getDescription(), generalModel.getModelRegistry())));
                    models.stream().filter(model -> Objects.equals(model.getType(), "programming")).findFirst().ifPresent(programmingModel -> list.add(new PromptModel(programmingModel.getModel(), null, null, "Coding/App Development", programmingModel.getSizeInGB().floatValue(), programmingModel.getDescription(), programmingModel.getModelRegistry())));
                    models.stream().filter(model -> Objects.equals(model.getType(), "vision")).findFirst().ifPresent(generalVision -> list.add(new PromptModel(generalVision.getModel(), null, null, "Image Understanding", generalVision.getSizeInGB().floatValue(), generalVision.getDescription(), generalVision.getModelRegistry())));
                }
            } catch (Exception ignored) {
            }
        }
        if (list.isEmpty()) {
            list.add(new PromptModel("llama3.2:3b", null, null, "Creative Writing/General Reasoning", 8f, "Meta Llama 3.2 for quick general tasks.", AiModelRegistry.OLLAMA.name()));
            list.add(new PromptModel("deepseek-coder:6.7b", null, null, "Programming/Code generation, explanation and debugging", 3.8f, "Lightweight Qwen2.5 3B variant targeting efficient.", AiModelRegistry.OLLAMA.name()));
            list.add(new PromptModel("qwen3-vl:4b", null, null, "Vision/Multimedia context understanding", 3.3f, "Lightweight Qwen2.5 3B variant targeting efficient.", AiModelRegistry.OLLAMA.name()));
        }
        return list;
    }

    /**
     * Retrieves a prompt template string from a JSON file based on the specified key.
     * The JSON file is expected to be named "prompts.json" and located in the classpath.
     * The method looks for a key in the JSON file and extracts the associated value
     * under the "prompt_string" field.
     *
     * @param key the key used to locate the desired prompt template in the JSON file
     * @return the prompt template string if the key and "prompt_string" field exist;
     * otherwise, returns an empty string
     * @throws RuntimeException if the JSON file is not found or an IO error occurs
     */
    public String getPromptTemplate(String key) {
        try {
            InputStream promptTemplates = new ClassPathResource("prompts.json").getInputStream();
            // Read as JsonNode to handle the nested object structure
            JsonNode rootNode = objectMapper.readTree(promptTemplates);
            if (rootNode.has(key) && rootNode.get(key).has("prompt_string")) {
                return rootNode.get(key).get("prompt_string").asText();
            }
            return "";
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Prompts file not found. " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error reading prompts file. " + e.getMessage(), e);
        }
    }

    /**
     * Generates a response from the model based on the provided system prompt, message history,
     * and user prompt. The method interacts with an OllamaClient to produce a response and
     * updates the message history with the assistant's reply for future context.
     *
     * @param model          the name or identifier of the model to be used for generating the response
     * @param messageHistory the list of previous messages exchanged in the conversation, maintaining context
     * @param userPrompt     the user's input or question that the model must respond to
     * @return the generated response from the model after processing the input prompts and history,
     * or a failure message if the response generation fails
     */
    public String generateResponseWithHistory(String model, List<ChatMessage> messageHistory, String userPrompt) {
        // Generate response
        String response = generateResponse(model, userPrompt, messageHistory, 0.7f);

        // Add the assistant response to history for future context
        if (response != null) {
            // Remove the <think> part
            if (response.contains("<think>")) {
                response = response.substring(response.indexOf("<think>") + 7, response.indexOf("</think>"));
            }
            return response;
        }
        return "Failed to generate a response";
    }

    /**
     * Prunes the chat history by removing messages that are too old or irrelevant to maintain a manageable size of
     * conversation history.
     * The messages that are pinned are not removed. The messages that are relatively new are summarized into bullet
     * points using a summarizer model
     *
     * @param messageHistory the list of previous messages exchanged in the conversation, maintaining context
     * @return the pruned list of chat messages, with potentially fewer entries
     */
    @Loggable
    public List<ChatMessage> pruneChatHistory(List<ChatMessage> messageHistory) {
        if (messageHistory == null || messageHistory.isEmpty()) return messageHistory;

        int totalLength = messageHistory.stream().mapToInt(x -> x.tokenLength).sum();
        if (totalLength <= TOKEN_HISTORY_LIMIT) return messageHistory;

        int averageLength = totalLength / messageHistory.size();
        // TODO: This is placeholder. Right now, the old messages that are shorter in length are being bluntly pruned.
        //  A proper summarizer model needs to be integrated for intelligent pruning
        for (ChatMessage message : messageHistory) {
            if (totalLength <= TOKEN_HISTORY_LIMIT) break;

            boolean initial = message.index < 2;
            boolean belowAverageLength = message.tokenLength < averageLength / 2;

            if (!message.pinned && !initial && belowAverageLength) {
                totalLength -= message.tokenLength;
                message.prunedContent = "";
                message.tokenLength = 0;
            }
        }
        return messageHistory;
    }

    /**
     * Retrieves a list of active chat sessions.
     * This method fetches all chat sessions from the repository
     * and filters them to include only sessions that are marked as active
     * (i.e., where the `voided` property is true).
     *
     * @return a list of active chat sessions.
     */
    @Transactional(readOnly = true)
    public List<ChatSession> getChatSessions() {
        List<ChatSession> chatSessions = chatSessionRepository.findAll();
        return chatSessions.stream().filter(session -> (session.getVoided() == null || !session.getVoided()) && !session.messages.isEmpty()).peek(session -> {
            session.messages.size(); // Force initialization of lazy collection
            if (session.project != null) {
                session.project.getName(); // Force initialization of project
            }
        }).sorted((s1, s2) -> {
            // First compare by starred status (starred sessions come first)
            if (s1.starred != s2.starred) {
                return s1.starred ? -1 : 1;
            }
            // Then compare by dateUpdated (most recent first)
            return s2.getDateUpdated().compareTo(s1.getDateUpdated());
        }).collect(Collectors.toList());
    }

    /**
     * Updates a given chat session by adding a new message and pruning its message history.
     * The updated chat session is then persisted to the repository.
     *
     * @param chatSession the chat session to update
     * @param message     the new message to add to the chat session
     * @param skipPersist whether to persist the updated chat session (disallowed during Private mode)
     * @return the updated chat session after adding the message and pruning the history
     */
    @Loggable
    @Transactional
    public ChatSession updateChatSession(ChatSession chatSession, ChatMessage message, String modelName, boolean skipPersist) {
        ChatSession sessionToUpdate = chatSession;

        if (!skipPersist && chatSession.id != null) {
            // Re-fetch only when persistence is allowed
            sessionToUpdate = chatSessionRepository.findById(chatSession.id).orElse(chatSession);
        }

        // Set the model name if provided
        if (modelName != null && !modelName.isBlank()) {
            sessionToUpdate.promptModel = modelName;
        }

        // If this is the first message, set the session name
        if (sessionToUpdate.messages.isEmpty()) {
            String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEE, dd MMM, yyyy - HH:mm:ss"));
            sessionToUpdate.setName(name);
        }
        // Check UUID to prevent duplicates in the list
        boolean exists = sessionToUpdate.messages.stream().anyMatch(m -> m.getUuid() != null && m.getUuid().equals(message.getUuid()));
        if (!exists) {
            sessionToUpdate.addMessage(message);
        }
        sessionToUpdate.messages = pruneChatHistory(sessionToUpdate.messages);
        if (skipPersist) {
            return sessionToUpdate;
        }

        // Notify new message event
        analyticsService.captureEvent(PosthogEvent.MESSAGE_SENT.name(), Map.of("message_id", message.getUuid()));

        return chatSessionRepository.save(sessionToUpdate);
    }

    /**
     * Renames an existing chat session by updating its name in the database.
     *
     * @param chatSession the chat session to be renamed; must contain a valid ID if it needs to be updated.
     * @param newName     the new name to assign to the chat session.
     * @return the updated chat session after renaming. If no valid ID is found, the original chat session is returned.
     */
    @Loggable
    @Transactional
    public ChatSession renameChatSession(ChatSession chatSession, String newName) {
        if (chatSession.id != null) {
            ChatSession sessionToUpdate = chatSessionRepository.findById(chatSession.id).orElse(chatSession);
            sessionToUpdate.setName(newName);
            chatSessionRepository.save(sessionToUpdate);
            // Notify Renamed Chat Session event
            analyticsService.captureEvent(PosthogEvent.CHAT_RENAMED.name(), Map.of("chat_session_id", sessionToUpdate.getUuid()));
        }
        return chatSession;
    }

    /**
     * Updates the starred status of a given chat session. This method is used to both Star and Unstar a chat session.
     *
     * @param chatSession the chat session to update; must not be null and must have a valid ID.
     * @param newStatus   the new value for the starred status; true to star the chat session, false to unstar it.
     * @return the updated chat session with the modified starred status.
     */
    @Loggable
    @Transactional
    public ChatSession starChatSession(ChatSession chatSession, boolean newStatus) {
        if (chatSession.id != null) {
            ChatSession sessionToUpdate = chatSessionRepository.findById(chatSession.id).orElse(chatSession);
            sessionToUpdate.starred = newStatus;
            if (newStatus) {
                // Notify Starred Chat Session event
                analyticsService.captureEvent(PosthogEvent.CHAT_STARRED.name(), Map.of("chat_session_id", sessionToUpdate.getUuid()));
            } else {
                // Notify Starred Chat Session event
                analyticsService.captureEvent(PosthogEvent.CHAT_UNSTARRED.name(), Map.of("chat_session_id", sessionToUpdate.getUuid()));
            }
            chatSessionRepository.save(sessionToUpdate);
        }
        return chatSession;
    }

    /**
     * Deletes a chat session and all associated messages from the repositories.
     *
     * @param chatSession the chat session to be deleted, along with its messages
     */
    @Loggable
    @Transactional
    public void deleteChatSession(ChatSession chatSession) {
        if (chatSession.id != null) {
            // Find the session to ensure it's managed and can be detached from project
            ChatSession session = chatSessionRepository.findById(chatSession.id).orElse(null);
            if (session != null) {
                if (session.project != null) {
                    session.project.chatSessions.remove(session);
                    session.project = null;
                }
                chatSessionRepository.delete(session);
                // Notify Starred Chat Session event
                analyticsService.captureEvent(PosthogEvent.CHAT_DELETED.name(), Map.of("chat_session_id", session.getUuid()));
            }
        } else {
            log.warn("Attempted to delete a chat session with no valid ID: {}", chatSession);
        }
    }

    /**
     * Searches for chat messages within a chat session that contain at least on token matching given pattern
     *
     * @param chatSession the chat session to search within
     * @param pattern     the pattern to match against chat message content
     * @return a list of chat messages that match the pattern
     */
    @Loggable
    public List<ChatMessage> searchInChatSession(ChatSession chatSession, String pattern, boolean caseSensitive) {
        if (pattern == null) {
            return new ArrayList<>();
        }
        return chatSession.messages.stream().filter(m -> m.content != null && (caseSensitive ? m.content : m.content.toLowerCase()).contains(caseSensitive ? pattern : pattern.toLowerCase())).collect(Collectors.toList());
    }

    /**
     * Retrieves all projects from the repository and ensures that associated
     * chat sessions, messages, and project names are initialized while the
     * current session is active.
     *
     * @return a list of all projects with their chat sessions and related data initialized
     */
    @Loggable
    @Transactional(readOnly = true)
    public List<Project> getProjects() {
        List<Project> projects = projectRepository.findAll();
        // Force initialization while the session is active
        projects.forEach(p -> {
            p.chatSessions.forEach(session -> {
                session.messages.size();
                if (session.project != null) {
                    session.project.getName();
                }
            });
        });
        return new ArrayList<>(projects);
    }

    @Loggable
    @Transactional
    public Project createProject(String name, String instructions) {
        Project project = new Project();
        project.name = name;
        project.instructions = instructions;
        // Notify project creation event
        analyticsService.captureEvent(PosthogEvent.PROJECT_CREATED.name(), Map.of("project_id", project.getUuid()));
        return projectRepository.save(project);
    }

    @Loggable
    @Transactional
    public Project updateProject(Project project) {
        return projectRepository.save(project);
    }

    @Loggable
    @Transactional
    public void deleteProject(Project project) {
        // Although cascade is set to DETACH, just in case setting session to null to avoid accidental deletion
        project.chatSessions = null;
        projectRepository.save(project);
        projectRepository.delete(project);
    }

    @Loggable
    @Transactional
    public void addChatSessionToProject(ChatSession chatSession, Project project) {
        ChatSession session = chatSessionRepository.findById(chatSession.id).orElseThrow(() -> new RuntimeException("Chat session not found"));
        session.project = project;
        chatSessionRepository.save(session);
    }

    @Loggable
    @Transactional
    public void removeChatSessionFromProject(ChatSession chatSession) {
        ChatSession session = chatSessionRepository.findById(chatSession.id).orElseThrow(() -> new RuntimeException("Chat session not found"));
        session.project = null;
        chatSessionRepository.save(session);
    }

    /**
     * Toggles the pinned status of a specific message.
     * Enforces the global limit on pinned messages per session.
     *
     * @param messageId The ID of the message to toggle.
     * @return The updated ChatMessage entity.
     * @throws IllegalStateException if attempting to pin a message when the limit is reached.
     */
    @Loggable
    @Transactional
    public ChatMessage toggleMessagePin(Long messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId).orElseThrow(() -> new RuntimeException("Message not found with id: " + messageId));

        boolean newStatus = !Boolean.TRUE.equals(message.pinned);


        if (newStatus) {
            ChatSession session = message.chatSession;
            // We are trying to pin. Check the limit first
            long pinnedCount = chatMessageRepository.countByChatSessionAndPinnedTrue(session);
            if (pinnedCount >= maxPinnedMessagesLimit) {
                throw new IllegalStateException("Cannot pin more than " + maxPinnedMessagesLimit + " messages.");
            }
            // Notify pinned message event
            analyticsService.captureEvent(PosthogEvent.MESSAGED_PINNED.name(), Map.of("message_id", message.getUuid()));
        } else {
            // Notify un-pinned message event
            analyticsService.captureEvent(PosthogEvent.MESSAGED_UNPINNED.name(), Map.of("message_id", message.getUuid()));
        }
        message.pinned = newStatus;
        return chatMessageRepository.save(message);
    }
}
