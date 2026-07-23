package ai.nextgpu.agent.service;

import ai.nextgpu.agent.exception.VisionGenerationException;
import ai.nextgpu.common.dto.AiModelDto;
import ai.nextgpu.common.model.ComfyUiModelFile;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.nextgpu.agent.model.Image;
import ai.nextgpu.common.model.PosthogEvent;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Vision service for ComfyUI image generation.
 *
 * Downloading of ComfyUI models is handled entirely by NextGpuAiService /
 * ModelDownloadService. This service is responsible only for:
 *   - Building and queuing ComfyUI workflows
 *   - Polling for results and retrieving generated images
 */
@Getter
@Service
public class NextGpuVisionService {

    private static final Logger log = LoggerFactory.getLogger(NextGpuVisionService.class);

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final String serverAddress;

    private final Path comfyBaseDir;

    private final AnalyticsService analyticsService;

    @Autowired
    public NextGpuVisionService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${comfy.api.url:localhost:8188}") String comfyUrl,
            @Value("${comfy.basedir:./agent/comfy/basedir}") String comfyBaseDir,
            AnalyticsService analyticsService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.serverAddress = comfyUrl;
        this.comfyBaseDir = Paths.get(comfyBaseDir);
        this.analyticsService = analyticsService;
    }

    // ----------------------------------------------------------------
    // ComfyUI API - low-level primitives
    // ----------------------------------------------------------------

    public JsonNode queuePrompt(Map<String, Object> workflow) {
        String url = (serverAddress.startsWith("http") ? "" : "http://") + serverAddress + "/prompt";
        Map<String, Object> payload = Map.of("prompt", workflow);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        JsonNode response;
        try {
            response = restTemplate.postForObject(url, entity, JsonNode.class);
        } catch (Exception e) {
            log.error("Failed to reach ComfyUI server at {} while queuing prompt", url, e);
            throw new VisionGenerationException(
                    "Unable to reach ComfyUI at " + serverAddress + ". Is it running inside WSL2?", e);
        }

        if (response != null && response.has("prompt_id")) {
            return response;
        }
        log.error("ComfyUI did not return a prompt_id for the queued workflow. Raw response: {}", response);
        throw new VisionGenerationException("ComfyUI rejected the workflow (no prompt_id returned).");
    }

    public JsonNode uploadImage(byte[] imageData, String filename) {
        String url = (serverAddress.startsWith("http") ? "" : "http://") + serverAddress + "/upload/image";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("image", new ByteArrayResource(imageData) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("overwrite", "true");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        return restTemplate.postForObject(url, requestEntity, JsonNode.class);
    }

    public JsonNode getHistory(String promptId) {
        String url = (serverAddress.startsWith("http") ? "" : "http://") + serverAddress + "/history/" + promptId;
        try {
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            log.error("Failed to fetch history for prompt {} from {}", promptId, url, e);
            throw new VisionGenerationException("Unable to fetch generation history for prompt " + promptId, e);
        }
    }

    /**
     * Checks whether a prompt has finished. Network hiccups while polling are treated
     * as "not complete yet" (and logged) rather than aborting the whole generation -
     * ComfyUI may simply still be warming up. A hard failure only surfaces once the
     * overall polling loop in {@link #textToImage} times out or the final history
     * fetch fails.
     */
    public boolean isPromptComplete(String promptId) {
        JsonNode history;
        try {
            history = getHistory(promptId);
        } catch (VisionGenerationException e) {
            log.warn("Transient error polling prompt {} status, will retry", promptId);
            return false;
        }
        if (history == null || !history.has(promptId)) {
            return false;
        }
        JsonNode h = history.get(promptId);
        return h.has("outputs") && h.get("outputs").elements().hasNext();
    }

    public byte[] getImage(String filename, String subfolder, String folderType) {
        String url = String.format("%s%s/view?filename=%s&subfolder=%s&type=%s",
                (serverAddress.startsWith("http") ? "" : "http://"), serverAddress,
                filename, subfolder, folderType);
        try {
            byte[] data = restTemplate.getForObject(url, byte[].class);
            if (data == null || data.length == 0) {
                log.error("ComfyUI returned empty image bytes for {}", url);
                throw new VisionGenerationException("ComfyUI returned an empty image for " + filename);
            }
            return data;
        } catch (VisionGenerationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download image {} from {}", filename, url, e);
            throw new VisionGenerationException("Failed to download generated image " + filename, e);
        }
    }

    // ----------------------------------------------------------------
    // Workflow construction
    //
    // Checkpoint, sampler, and scheduler are derived directly from the
    // AiModelDto rather than from a JSON template file.
    // ----------------------------------------------------------------

    /**
     * Convenience overload - uses default steps (30), temperature (0.75), denoise (1.0).
     */
    public Map<String, Object> createWorkflow(
            String positivePrompt,
            String negativePrompt,
            int width,
            int height,
            AiModelDto model) throws IOException {
        return createWorkflow(positivePrompt, negativePrompt, width, height, model, 30, 0.75, 1f);
    }

    /**
     * Builds a full KSampler workflow from the given AiModelDto.
     *
     * <p>Checkpoint resolution order:
     * <ol>
     *   <li>First file in {@code model.files} where {@code targetSubfolder} is
     *       {@code "checkpoints"} or {@code "diffusion_models"}</li>
     *   <li>Hard-coded fallback: {@code "v1-5-pruned-emaonly.safetensors"}</li>
     * </ol>
     *
     * <p>Sampler and scheduler come from {@link AiModelDto#getSampler()} /
     * {@link AiModelDto#getScheduler()}, falling back to {@code "euler"} /
     * {@code "normal"} if null.
     */
    public Map<String, Object> createWorkflow(
            String positivePrompt,
            String negativePrompt,
            int width,
            int height,
            AiModelDto model,
            int steps,
            double temperature,
            double denoise) throws IOException {

        // --- Derive generation parameters from the DTO ---
        String checkpointName = "v1-5-pruned-emaonly.safetensors"; // fallback
        if (model != null && model.getFiles() != null) {
            for (ComfyUiModelFile file : model.getFiles()) {
                String subfolder = file.getTargetSubfolder();
                if ("checkpoints".equals(subfolder) || "diffusion_models".equals(subfolder)) {
                    checkpointName = file.getFileName();
                    break;
                }
            }
        }

        String sampler   = (model != null && model.getSampler()   != null) ? model.getSampler()   : "euler";
        String scheduler = (model != null && model.getScheduler() != null) ? model.getScheduler() : "normal";

        // --- Build the ComfyUI workflow graph ---
        ObjectNode workflow = objectMapper.createObjectNode();

        // Checkpoint loader
        ObjectNode ckpt = objectMapper.createObjectNode();
        ckpt.put("class_type", "CheckpointLoaderSimple");
        ckpt.set("inputs", objectMapper.createObjectNode().put("ckpt_name", checkpointName));
        workflow.set("ckpt", ckpt);

        // Positive prompt
        ObjectNode posInputs = objectMapper.createObjectNode();
        posInputs.put("text", positivePrompt);
        posInputs.set("clip", objectMapper.createArrayNode().add("ckpt").add(1));
        ObjectNode pos = objectMapper.createObjectNode();
        pos.put("class_type", "CLIPTextEncode");
        pos.set("inputs", posInputs);
        workflow.set("pos", pos);

        // Negative prompt
        ObjectNode negInputs = objectMapper.createObjectNode();
        negInputs.put("text", negativePrompt);
        negInputs.set("clip", objectMapper.createArrayNode().add("ckpt").add(1));
        ObjectNode neg = objectMapper.createObjectNode();
        neg.put("class_type", "CLIPTextEncode");
        neg.set("inputs", negInputs);
        workflow.set("neg", neg);

        // Latent image
        ObjectNode latentInputs = objectMapper.createObjectNode();
        latentInputs.put("width",      width);
        latentInputs.put("height",     height);
        latentInputs.put("batch_size", 1);
        ObjectNode latent = objectMapper.createObjectNode();
        latent.put("class_type", "EmptyLatentImage");
        latent.set("inputs", latentInputs);
        workflow.set("latent", latent);

        // KSampler
        ObjectNode sampleInputs = objectMapper.createObjectNode();
        sampleInputs.set("model",        objectMapper.createArrayNode().add("ckpt").add(0));
        sampleInputs.set("positive",     objectMapper.createArrayNode().add("pos").add(0));
        sampleInputs.set("negative",     objectMapper.createArrayNode().add("neg").add(0));
        sampleInputs.set("latent_image", objectMapper.createArrayNode().add("latent").add(0));
        sampleInputs.put("seed",         42);
        sampleInputs.put("steps",        steps);
        sampleInputs.put("cfg",          temperature * 10);
        sampleInputs.put("sampler_name", sampler);
        sampleInputs.put("scheduler",    scheduler);
        sampleInputs.put("denoise",      denoise);
        ObjectNode sample = objectMapper.createObjectNode();
        sample.put("class_type", "KSampler");
        sample.set("inputs", sampleInputs);
        workflow.set("sample", sample);

        // VAE decode
        ObjectNode decodeInputs = objectMapper.createObjectNode();
        decodeInputs.set("samples", objectMapper.createArrayNode().add("sample").add(0));
        decodeInputs.set("vae",     objectMapper.createArrayNode().add("ckpt").add(2));
        ObjectNode decode = objectMapper.createObjectNode();
        decode.put("class_type", "VAEDecode");
        decode.set("inputs", decodeInputs);
        workflow.set("decode", decode);

        // Image nodes - shared inputs object
        ObjectNode saveInputs = objectMapper.createObjectNode();
        saveInputs.set("images", objectMapper.createArrayNode().add("decode").add(0));
        saveInputs.put("filename_prefix", "ComfyUI");

        // PreviewImage saves to the temp directory and returns the image
        ObjectNode preview = objectMapper.createObjectNode();
        preview.put("class_type", "PreviewImage");
        preview.set("inputs", saveInputs);
        workflow.set("preview", preview);

        // SaveImage writes to the output directory
        ObjectNode save = objectMapper.createObjectNode();
        save.put("class_type", "SaveImage");
        save.set("inputs", saveInputs);
        workflow.set("save", save);

        @SuppressWarnings("unchecked")
        Map<String, Object> workflowMap = objectMapper.convertValue(workflow, Map.class);
        return workflowMap;
    }

    // ----------------------------------------------------------------
    // Text-to-image entry point
    // ----------------------------------------------------------------

    /**
     * Generates an image from a text prompt using the given installed ComfyUI model.
     *
     * <p>The model must already be present on disk.
     * Use {@code ModelDownloadService} to download models before calling this.
     *
     * @param positivePrompt what to include in the generated image
     * @param negativePrompt what to exclude (may be empty)
     * @param width          output width in pixels
     * @param height         output height in pixels
     * @param model          installed ComfyUI model (provides checkpoint, sampler, scheduler)
     * @return filename of the generated PNG relative to the comfy output directory.
     *         Never returns {@code null} - any failure is logged here and thrown as a
     *         {@link VisionGenerationException} describing exactly what went wrong.
     */
    public String textToImage(
            String positivePrompt,
            String negativePrompt,
            int width,
            int height,
            AiModelDto model) throws IOException, InterruptedException {

        Map<String, Object> workflow = createWorkflow(positivePrompt, negativePrompt, width, height, model);

        JsonNode queueResponse = queuePrompt(workflow);
        String promptId = queueResponse.get("prompt_id").asText();
        log.info("Queued textToImage prompt with ID: {}", promptId);

        // Poll until complete (max 60 attempts, 1 s apart => up to 60s)
        int maxAttempts = 60;
        int attempts = 0;
        boolean completed = false;
        while (attempts < maxAttempts) {
            if (isPromptComplete(promptId)) {
                completed = true;
                log.info("Prompt {} complete (via polling, after {}s)", promptId, attempts);
                break;
            }
            TimeUnit.SECONDS.sleep(1);
            attempts++;
        }

        if (!completed) {
            log.error("Timed out after {}s waiting for ComfyUI to finish prompt {}", maxAttempts, promptId);
            throw new VisionGenerationException(
                    "Timed out waiting for ComfyUI to generate the image (prompt " + promptId + ").");
        }

        JsonNode response = getHistory(promptId);
        if (response == null || !response.has(promptId)) {
            log.error("No history entry returned by ComfyUI for completed prompt {}", promptId);
            throw new VisionGenerationException("ComfyUI reported no history for prompt " + promptId);
        }

        JsonNode history = response.get(promptId);
        if (history == null || !history.has("outputs")) {
            log.error("History for prompt {} is missing an 'outputs' field: {}", promptId, history);
            throw new VisionGenerationException("ComfyUI history for prompt " + promptId + " has no outputs.");
        }

        JsonNode outputs = history.get("outputs");

        // Prefer type == "output"; fall back to the first available image node
        JsonNode bestImageInfo     = null;
        JsonNode fallbackImageInfo = null;

        Iterator<String> fieldNames = outputs.fieldNames();
        while (fieldNames.hasNext()) {
            String nodeName    = fieldNames.next();
            JsonNode nodeOutput = outputs.get(nodeName);
            if (!nodeOutput.has("images")
                    || !nodeOutput.get("images").isArray()
                    || nodeOutput.get("images").isEmpty()) {
                continue;
            }
            JsonNode imageInfo = nodeOutput.get("images").get(0);
            if (fallbackImageInfo == null) {
                fallbackImageInfo = imageInfo;
            }
            if ("output".equalsIgnoreCase(imageInfo.path("type").asText(""))) {
                bestImageInfo = imageInfo;
                break;
            }
        }

        JsonNode imageInfo = (bestImageInfo != null) ? bestImageInfo : fallbackImageInfo;
        if (imageInfo == null) {
            log.error("Prompt {} completed but produced no image outputs. Outputs: {}", promptId, outputs);
            throw new VisionGenerationException("ComfyUI finished but produced no image for prompt " + promptId);
        }

        byte[] imageData = getImage(
                imageInfo.get("filename").asText(),
                imageInfo.get("subfolder").asText(),
                imageInfo.get("type").asText()
        );

        BufferedImage bufferedImage;
        try {
            bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException e) {
            log.error("Failed to decode {} bytes of image data for prompt {}", imageData.length, promptId, e);
            throw new VisionGenerationException("Failed to decode generated image for prompt " + promptId, e);
        }
        if (bufferedImage == null) {
            log.error("ImageIO could not decode image data for prompt {} (unrecognized format, {} bytes)",
                    promptId, imageData.length);
            throw new VisionGenerationException(
                    "Downloaded image for prompt " + promptId + " could not be decoded.");
        }

        Path outputDir  = comfyBaseDir.resolve("output");
        Files.createDirectories(outputDir);
        String filename = promptId + ".png";
        Path outputFile = outputDir.resolve(filename);
        try {
            ImageIO.write(bufferedImage, "png", outputFile.toFile());
        } catch (IOException e) {
            log.error("Error writing image {} to {}", filename, outputFile, e);
            throw new VisionGenerationException("Failed to save generated image to disk: " + outputFile, e);
        }

        try {
            analyticsService.captureEvent(PosthogEvent.IMAGE_GENERATED.name(), Map.of("file_name", filename));
        } catch (Exception e) {
            // Analytics is best-effort and must never fail an otherwise successful generation.
            log.warn("Failed to capture analytics event for prompt {}", promptId, e);
        }

        log.info("textToImage succeeded for prompt {} -> {}", promptId, filename);
        return filename;
    }

    // ----------------------------------------------------------------
    // TODO stubs - future capabilities
    // ----------------------------------------------------------------

    public String classifyImage(Image image) {
        return null;
    }

    public Map<String, String> classifyImageLabels() {
        return null;
    }

    public Map<String, Integer> identifyImageObjects() {
        return null;
    }

    public String extractText(Image image) {
        return null;
    }

    public String upscaleImage(Image image) {
        return null;
    }

    public String rescaleImage(Image image) {
        return null;
    }

    public String colourizeImage(Image image) {
        return null;
    }

    public String imageToImage(Image image, String prompt) {
        return null;
    }

    public String retrieveImageContext(Image image) {
        return null;
    }

    public String imageToPrompt(Image image) {
        return null;
    }
}