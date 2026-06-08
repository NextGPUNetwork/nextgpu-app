package ai.nextgpu.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.*;
import ai.nextgpu.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for interacting with NextGPU web API endpoints
 */
@Slf4j
@Component
public class NextGpuWebService {

    @Value("${nextgpu.web.baseUrl:http://localhost:8080}")
    private String BASE_URL;

    @Value("${nextgpu.web.versionBaseUrl:http://localhost:8080}")
    private String VERSION_BASE_URL;
    
    private final HttpUtil httpUtil;

    @Autowired
    public NextGpuWebService(HttpUtil httpUtil) {
        this.httpUtil = httpUtil;
    }

    /* *********************************/
    /* ***** Provider Onboarding ***** */
    /* *********************************/

    /**
     * Retrieves the user profile associated with the specified wallet address.
     *
     * @param walletAddress the wallet address of the user whose profile is to be retrieved
     * @return a {@code UserProfileDto} containing the user's profile information
     * @throws Exception if an error occurs during the API communication or response deserialization
     */
    public UserProfileDto getUserProfile(String walletAddress) throws Exception {
        String url = this.BASE_URL + "/users/" + walletAddress;
        return httpUtil.get(url, UserProfileDto.class, true);
    }

    /**
     * Verifies the given OTP (One-Time Password) against the specified wallet address.
     *
     * @param walletAddress The wallet address to be verified.
     * @param otp The One-Time Password to be validated.
     * @return {@code true} if the OTP is successfully verified, {@code false} otherwise.
     * @throws Exception If an error occurs during the HTTP request or response processing.
     */
    public AuthResponseDto verifyOtp(String walletAddress, String otp) throws Exception {
        String url = this.BASE_URL + "/users/otp/verify";
        Map<String, String> body = new HashMap<>();
        body.put("walletAddress", walletAddress);
        body.put("otp", otp);
        return httpUtil.post(url, body, AuthResponseDto.class, false);
    }

    /* ************************************/
    /* ***** Computer API ENDPOINTS ***** */
    /* ************************************/

    public ComputerDto createComputer(CreateComputerDto createDto) throws Exception {
        String url = this.BASE_URL + "/computers";
        return httpUtil.post(url, createDto, ComputerDto.class, true);
    }

    /* **************************************************/
    /* ** Benchmark and Audit API ENDPOINTS ** */
    /* **************************************************/

    public void saveBenchmarkReport(BenchmarkReportDto reportDto) throws Exception {
        String url = this.BASE_URL + "/reports/benchmark";

        httpUtil.post(url, reportDto, BenchmarkReportDto.class, true);
    }


    /**
     * Audits a given computer system for anomalies by sending its details to the anomaly reporting service.
     *
     * @param computerDto the data transfer object containing detailed information about the computer to be audited
     * @return {@code true} if the audit passes successfully, {@code false} otherwise
     */
    public boolean auditComputerForAnomalies(ComputerDto computerDto) {
        String url = this.BASE_URL + "/reports/anomaly";
        try {
            String responseMessage = httpUtil.post(url, computerDto, String.class, true);
            return responseMessage.equals("Audit passed successfully.");
        } catch (Exception e) {
            log.error("Anomaly audit failed for computer: {}", computerDto.getUuid(), e);
            return false;
        }
    }

    /* *********************************/
    /* ** Ollama AI Model Endpoints ** */
    /* *********************************/

    /**
     * Get available AI models filtered by GPU specifications
     *
     * @param gpuDto The GPU specifications to filter models by
     * @return List of compatible AI models for the given GPU
     * @throws Exception If there's an error communicating with the API
     */
    public List<AiModelDto> getAvailableModelsByGpu(GpuDto gpuDto) throws Exception {
        String url = this.BASE_URL + "/api/ai/models";

        // Send a POST request with GPU DTO as the body
        String response = httpUtil.post(url, gpuDto, String.class, true);

        // Parse the response into List<OllamaModelDto>
        return JsonUtil.OBJECT_MAPPER.readValue(response, new TypeReference<>() {
        });
    }


    /**
     * Retrieves a list of all available AI models from the server.
     *
     * @return a list of {@code AiModelDto} objects representing the available AI models,
     *         or an empty list if an error occurs during the API communication or response deserialization.
     */
    public List<AiModelDto> getAllAvailableModels() {
        try {
            String url = this.BASE_URL + "/api/ai/models";
            // Send GET request
            List<AiModelDto> models = JsonUtil.OBJECT_MAPPER.readValue(
                    httpUtil.get(url, null),
                    new TypeReference<List<AiModelDto>>() {
                    }
            );

            // TODO: Remove temporary filter later on.
            return models.stream()
                    .filter(m ->
                            !Objects.equals(m.getFullName(), "sdxl-turbo") &&
                                    !Objects.equals(m.getFullName(), "stable-audio-open-1.0_repackaged")).toList();

        } catch (Exception e) {
            log.error("Error fetching available models: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Sends a batch of event data associated with a specific machine to the analytics API.
     *
     * @param machineHash a unique identifier representing the machine whose events are being posted
     * @param events a map containing event names of events as keys and their corresponding properties as values,
     *               where each property's value is represented as another map of key-value pairs (the actual event data)
     *               where the key is the property name and the value is the property value
     *               Map(Event Name -> Event Data) ==> Map<String, Object> where Object is Map<String, Object>
     * @throws Exception if an error occurs during the HTTP request or while processing the response
     */
    public void postEventDataInBatch(String machineHash, Map<String, Object> events) throws Exception {
        String url = this.BASE_URL + "/api/analytics/" + machineHash;
        httpUtil.post(url, Map.of("events", events), null, false);
    }

    /**
     * Checks for the latest available version of the application.
     *
     * @return a {@code VersionCheckDto} containing the latest version information.
     * @throws Exception if an error occurs during the API communication.
     */
    public VersionCheckDto getLatestVersion() throws Exception {
        String url = this.VERSION_BASE_URL + "/api/installer";
        return httpUtil.get(url, VersionCheckDto.class, true);
    }


}
