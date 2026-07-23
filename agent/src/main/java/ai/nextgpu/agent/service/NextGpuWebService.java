package ai.nextgpu.agent.service;

import ai.nextgpu.agent.dto.SttToolHealthResponseDto;
import ai.nextgpu.common.exception.ErrorCode;
import ai.nextgpu.common.exception.ValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.*;
import ai.nextgpu.common.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Service for interacting with NextGPU web API endpoints
 */
@Component
public class NextGpuWebService {

    private static final Logger log = LoggerFactory.getLogger(NextGpuWebService.class);

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
     * @return a {@code UserDto} containing the user's profile information
     * @throws Exception if an error occurs during the API communication or response deserialization
     */
    public UserDto getUserProfile(String walletAddress) throws Exception {
        String url = this.BASE_URL + "/users/" + walletAddress;
        return httpUtil.get(url, UserDto.class, true);
    }

    public List<ProviderAttributeTypeDto> getProviderAttributeTypes() {
        try {
            String url = this.BASE_URL + "/providers/attributetypes";
            return httpUtil.get(url, new TypeReference<List<ProviderAttributeTypeDto>>() {}, true);
        } catch (Exception e) {
            log.error("Error fetching provider attribute types: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Verifies the given OTP (One-Time Password) against the specified wallet address.
     *
     * @param otp The One-Time Password to be validated.
     * @return {@code true} if the OTP is successfully verified, {@code false} otherwise.
     */
    public AuthResponseDto verifyOtp(String otp) {
        try {
            log.info("OTP {}",otp);
            String url = this.BASE_URL + "/users/otp/access/verify";
            return httpUtil.post(url, new OtpDto(otp), AuthResponseDto.class, false);
        }
        catch (Exception e) {
            throw new ValidationException(ErrorCode.INVALID_OTP.getDescription(), ErrorCode.INVALID_OTP, e);
        }
    }

    /* ************************************/
    /* ***** Computer API ENDPOINTS ***** */
    /* ************************************/

    /**
     * Retrieves detailed information about a computer with the specified UUID.
     *
     * @param uuid the unique identifier of the computer to be retrieved
     * @return a {@code ComputerDto} containing the computer's details
     * @throws Exception if an error occurs during the API communication or response deserialization
     */
    public ComputerDto getComputer(String uuid) throws Exception {
        String url = this.BASE_URL + "/computers/" + uuid;
        return httpUtil.get(url, ComputerDto.class, true);
    }

    /**
     * Creates a new computer using the provided data transfer object.
     *
     * @param createDto the data transfer object containing the details required to create a computer
     * @return a {@code ComputerDto} object representing the created computer
     * @throws Exception if an error occurs during the API communication or response processing
     */
    public ComputerDto createComputer(CreateComputerDto createDto) throws Exception {
        try {
            String url = this.BASE_URL + "/computers";
            return httpUtil.post(url, createDto, ComputerDto.class, true);
        } catch (Exception e) {
            log.error("Error creating computer: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<ComputerAttributeTypeDto> getComputerAttributeTypes() {
        try {
            String url = this.BASE_URL + "/computers/attributetypes";
            return httpUtil.get(url, new TypeReference<List<ComputerAttributeTypeDto>>() {}, true);
        } catch (Exception e) {
            log.error("Error fetching computer attribute types: {}", e.getMessage());
            return List.of();
        }
    }

    /* **********************************************************/
    /* ** Benchmark/Hardware Reports and Audit API ENDPOINTS ** */
    /* **********************************************************/

    /**
     * Saves the hardware report by sending it to the specified endpoint.
     *
     * @param reportDto the data transfer object containing the hardware report details
     * @throws Exception if an error occurs during the HTTP POST request or response processing
     */
    public void saveHardwareReport(HardwareReportDto reportDto) throws Exception {
        String url = this.BASE_URL + "/reports/hardware";
        httpUtil.post(url, reportDto, HardwareReportDto.class, true);
    }

    /**
     * Saves the benchmark report by sending it to the specified endpoint.
     *
     * @param reportDto the data transfer object containing the benchmark report details
     * @throws Exception if an error occurs during the HTTP POST request or response processing
     */
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
    @SuppressWarnings("unchecked")
    public boolean auditComputerForAnomalies(ComputerDto computerDto) throws Exception {
        String url = this.BASE_URL + "/reports/audit";
        Map<String, String> responseMessage = httpUtil.post(url, computerDto, Map.class, true);
        return responseMessage.get("audit_status").equals("Passed");

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
            List<AiModelDto> models = httpUtil.get(url, new TypeReference<>() {}, false);

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
     *               Map (Event Name -> Event Data) ==> Map<String, Object> where Object is Map<String, Object>
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


    public boolean isSttServiceAvailable() {
        try {
            SttToolHealthResponseDto result = httpUtil.get("http://localhost:8177/health", SttToolHealthResponseDto.class, false);
            return Objects.equals(result.getStatus().toLowerCase(), "ok");
        } catch (Exception e) {
            throw new RuntimeException("Your local speech-to-text tool is not reachable.");
        }
    }

    public SttDto getTransformedString(File audioFile) throws Exception {
        String url = "http://localhost:8177/v1/audio/transcriptions";
        return httpUtil.postMultipart(url, Map.of("file", audioFile), SttDto.class, false);
    }


}
