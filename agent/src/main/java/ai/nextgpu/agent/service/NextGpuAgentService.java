package ai.nextgpu.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.*;
import ai.nextgpu.common.model.*;
import ai.nextgpu.common.dto.*;
import ai.nextgpu.common.report.*;
import ai.nextgpu.common.util.JsonUtil;
import ai.nextgpu.common.util.StringUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Service class responsible for handling operations related to GPU agents, WebSocket communication,
 * authentication, reporting, and other ancillary services. It serves as the central point of interaction
 * for GPU-related functionalities and provides methods for managing global properties, user authentication,
 * WebSocket communication, hardware detection, and resource recommendations.
 */
@Slf4j
@Getter
@Service
public class NextGpuAgentService {

    private boolean isLoggedIn = false;

    private String loginWallet = null;

    private final ObjectMapper objectMapper = JsonUtil.OBJECT_MAPPER;

    @Value("${nextgpu.web.baseUrl:http://localhost:8080}")
    private String BASE_URL;

    @Value("${nextgpu.web.socketUrl:ws://localhost:8080/ws}")
    private String SOCKET_URL;

    private final GlobalPropertyRepository globalPropertyRepository;
    private final DataService dataService;
    private final WebSocketMessageService webSocketMessageService;
    private final HardwareUtil hardwareUtil;
    private final ComputerService computerService;
    private final AnalyticsService analyticsService;

    @Getter
    private WebSocketStompClient webSocketClient;

    @Autowired
    private NextGpuWebService nextGpuWebService;

    @Autowired
    public NextGpuAgentService(
            GlobalPropertyRepository globalPropertyRepository,
            DataService dataService,
            @Lazy WebSocketMessageService webSocketMessageService,
            @Lazy HardwareUtil hardwareUtil,
            ComputerService computerService, AnalyticsService analyticsService) {
        this.globalPropertyRepository = globalPropertyRepository;
        this.dataService = dataService;
        this.webSocketMessageService = webSocketMessageService;
        this.hardwareUtil = hardwareUtil;
        this.computerService = computerService;
        this.analyticsService = analyticsService;
        checkEnvironment();
    }

    /**
     * Verifies if WSL is running.
     * @return true if the environment is ready, false otherwise.
     */
    public void checkEnvironment() {
        if (OSUtil.IS_WINDOWS) {
            GlobalProperty setupComplete = getGlobalProperty(GlobalPropertyConfig.IS_SETUP_COMPLETED);
            if (setupComplete != null && setupComplete.getValue() == Boolean.TRUE) {
                // WSL startup is now handled asynchronously in NextGpuAgentApplication to avoid blocking Spring startup
                log.info("WSL startup will be handled by the application UI flow.");
            }
        }
    }

    /**
     * Retrieves a global property by its name.
     *
     * @param name the name of the global property to retrieve
     * @return the global property associated with the specified name,
     *         or null if no property with the given name is found
     */
    public GlobalProperty getGlobalProperty(String name) {
        return globalPropertyRepository.findByName(name).orElse(null);
    }

    /**
     * Saves or updates a global property with the specified name, value, and datatype.
     * If a global property with the given name already exists, it updates the value reference
     * and persists the changes. Otherwise, it creates and saves a new global property.
     *
     * @param name     the name of the global property to be saved or updated
     * @param value    the value reference to be assigned to the global property
     * @param datatype the datatype of the global property
     * @return the saved or updated {@code GlobalProperty} object
     */
    public GlobalProperty saveGlobalProperty(String name, String value, String datatype) {
        Optional<GlobalProperty> globalProperty = globalPropertyRepository.findByName(name);
        if (globalProperty.isPresent()) {
            globalProperty.get().setValueReference(value);
            globalPropertyRepository.save(globalProperty.get());
            return globalProperty.get();
        } else {
            GlobalProperty newGlobalProperty = new GlobalProperty();
            newGlobalProperty.setName(name);
            newGlobalProperty.setDatatype(datatype);
            newGlobalProperty.setValueReference(value);
            return globalPropertyRepository.save(newGlobalProperty);
        }
    }

    /**
     * Saves the given global property to the repository.
     *
     * @param globalProperty the global property to be saved
     * @return the saved instance of the global property
     */
    @Loggable
    public GlobalProperty saveGlobalProperty(GlobalProperty globalProperty) {
        return globalPropertyRepository.save(globalProperty);
    }

    /**
     * Automatically logs in the user based on a stored JWT token.
     * This method attempts to fetch a previously stored JWT token from the
     * global property repository. If the token is present and valid, it validates the token
     * by decoding its payload and checking whether the token is expired.
     * If the token is valid and has not expired, the user is marked as logged in
     */
    @Loggable
    public void autoLogin() {
        // Find the JWT_TOKEN property from the repository
        GlobalProperty jwtTokenProperty = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);
        if (jwtTokenProperty != null && jwtTokenProperty.getValueReference() != null) {
            String token = jwtTokenProperty.getValueReference();
            if (token.isEmpty()) {
                return;
            }
            // Validate the JWT token by checking if it is expired
            try {
                // Decode the token and extract the expiration time
                String[] parts = token.split("\\.");
                if (parts.length < 3) {
                    return;
                }
                String payload = new String(Base64.getDecoder().decode(parts[1]));
                // Parse the payload to extract the expiration timestamp
                @SuppressWarnings("unchecked")
                Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
                Integer exp = (Integer) payloadMap.get("exp");
                isLoggedIn = LocalDateTime.now().isBefore(LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC));
                if (isLoggedIn) {
                    GlobalProperty uuidProperty = getGlobalProperty(GlobalPropertyConfig.COMPUTER_UUID);
                    if (uuidProperty != null) {
                        GlobalProperty walletProperty = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);
                        if(walletProperty != null)
                            loginWallet = walletProperty.getValueReference();
                        GlobalProperty jwtProperty = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);

                        if (!initializeWebSocketClient(uuidProperty.getValueReference(), jwtProperty.getValueReference())) {
                            String errorMessage = "Authentication successful, but failed to initialize WebSocket client";
                            log.error("Authentication successful, but failed to initialize WebSocket client");
                            webSocketMessageService.invalidateSessionKeyPair();
                            throw new RuntimeException(errorMessage);
                        }
                    }
                }
            } catch (Exception ignored) {
                webSocketMessageService.invalidateSessionKeyPair();
//                isLoggedIn = false;
            }
        }
    }

    /**
     * Checks if the user is currently logged in.
     * Note! This method was introduced due to incompatibility with Lombok's @Getter annotation with Kotlin.
     *
     * @return true if the user is logged in, false otherwise
     */
    public boolean loggedIn() {
        return isLoggedIn;
    }

    /**
     * Authenticates a user by sending their wallet address and a one-time key to a remote API.
     * If authentication is successful, the JWT token from the response is saved as a global property.
     *
     * @param walletAddress the user's wallet address to be authenticated
     * @param oneTimeKey    a one-time key associated with the user's session or authentication attempt
     * @return true if authentication is successful, false otherwise
     */
    public boolean authenticate(String walletAddress, String oneTimeKey) {
        try {
            AuthResponseDto response = nextGpuWebService.verifyOtp(walletAddress, oneTimeKey);
            saveGlobalProperty(GlobalPropertyConfig.JWT_TOKEN, response.getAccessToken(), "java.lang.String");
            saveGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET, walletAddress, "java.lang.String");
            Provider provider = dataService.findProviderByWalletAddress(walletAddress);
            User responseUser = response.getUser();
            if (provider == null && responseUser instanceof Provider source) {
                Provider newProvider = new Provider();
                BeanUtils.copyProperties(source, newProvider, "id", "createdAt", "updatedAt");
                // enforce consistency
                newProvider.setWalletAddress(walletAddress);

                dataService.saveProvider(newProvider);
            }
            autoLogin();
            return true;
        } catch (Exception e) {
            webSocketMessageService.invalidateSessionKeyPair();
            throw new RuntimeException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Logs out the user by removing their JWT token from the database.
     */
    @Loggable
    public void logout() {
        // Disconnect WebSocket if connected
        if (webSocketClient != null) {
            try {
                // Get the username should be associated with the JWT token
                GlobalProperty property = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);
                if (property != null) {
                    String username = property.getValueReference();
                    webSocketClient.disconnect(username);
                    webSocketClient = null;
                }
            } catch (Exception e) {
                log.error("Error disconnecting WebSocket: {}", e.getMessage());
            } finally {
                webSocketMessageService.invalidateSessionKeyPair();
                saveGlobalProperty(GlobalPropertyConfig.JWT_TOKEN, null, "java.lang.String");
            }
        }
    }

    @Loggable
    public boolean nukeInstance() {
        try {
            // Logout first. Disconnect active sessions
            logout();
            if (OSUtil.IS_WINDOWS) {
                String distro = getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO).getValueReference();
                String username = getGlobalProperty(GlobalPropertyConfig.OS_USERNAME).getValueReference();
                String password = getGlobalProperty(GlobalPropertyConfig.OS_PASSWORD).getValueReference();
                // Unregister the WSL instance to delete it
                if (distro != null) {
                    String existing = OSUtil.executeCommand("wsl --list --quiet");
                    if (existing != null && existing.contains(distro)) {
                        OSUtil.executeCommand("wsl --unregister " + distro);
                    }
                }
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData != null && !localAppData.isBlank()) {
                    Path nextGpuHome = Paths.get(localAppData, "NextGPU");
                    Path scriptState = nextGpuHome.resolve("state.json");
                    Path scriptCreds = nextGpuHome.resolve("wsl_credentials.txt");
                    Path scriptLogs = nextGpuHome.resolve("install_debug.log");
                    Files.deleteIfExists(scriptState);
                    Files.deleteIfExists(scriptCreds);
                    Files.deleteIfExists(scriptLogs);
                }
                // Delete log files
                Files.deleteIfExists(java.nio.file.Paths.get("logs", "nextgpu.log"));
            }
            // Flush the internal database
            dataService.purgeDatabase();
        } catch (Exception e) {
            log.error("Nuke instance failed: {}", e.getMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Saves the operating system credentials (username and password) as global properties
     * in the repository. If the properties already exist, their values are updated; otherwise,
     * new properties are created and saved.
     *
     * @param username the username for the operating environment (e.g., Linux/WSL Distro)
     * @param password the password for the specified username
     * @return true if the credentials are successfully saved, false otherwise
     */
    public boolean saveOSCredentials(String username, String password) {
        try {
            saveGlobalProperty(GlobalPropertyConfig.OS_USERNAME, username, "java.lang.String");
            saveGlobalProperty(GlobalPropertyConfig.OS_PASSWORD, password, "java.lang.String");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /* *********************** */
    /* * Web Socket Services * */
    /* *********************** */

    /**
     * Initializes the WebSocket client for the given Provider Personal Computer (PPC) UUID.
     *
     * @param computerUuid the UUID of the Provider Personal Computer (PPC) to be used for the WebSocket connection
     * @return true if the WebSocket client is successfully initialized, false if the user is not logged in
     * @throws RuntimeException if an error occurs during the initialization process
     */
    public boolean initializeWebSocketClient(String computerUuid, String jwtToken) throws RuntimeException {
        if (!isLoggedIn()) {
            webSocketMessageService.invalidateSessionKeyPair();
            log.warn("Cannot initialize WebSocket client: User is not logged in");
            return false;
        }

        try {
            // Get JWT token to confirm it exists
            GlobalProperty jwtTokenProperty = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);
            if (jwtTokenProperty == null || jwtTokenProperty.getValueReference() == null ||
                    jwtTokenProperty.getValueReference().isEmpty()) {
                log.error("Cannot initialize WebSocket client: JWT token is missing");
                return false;
            }

            // IMPORTANT: new session => new Kyber keypair
            webSocketMessageService.rotateSessionKeyPair();

            /* ** IMPORTANT **
             * Passing the PPC UUID as the username to the WebSocket client,
             * The UUID will be used for communication with this PPC instance from the WebSocket server
             */
            this.webSocketClient = new WebSocketStompClient(webSocketMessageService, computerUuid, SOCKET_URL, jwtToken);
            log.info("WebSocket client initialized successfully for PPC with UUID: {}", computerUuid);
            return true;
        } catch (Exception e) {
            webSocketMessageService.invalidateSessionKeyPair();
            log.error("Failed to initialize WebSocket client for PPC with UUID {}: {}", computerUuid, e.getMessage());
            // Don't throw exception - just return false to handle the error gracefully
            return false;
        }
    }

    /**
     * Invoked only when the provider is ready to serve. Sends a WebSocket message by creating a WebSocketMessageDto object and
     * passing it to the appropriate WebSocket transmission method.
     *
     * @param message     The message content to be sent over the WebSocket.
     * @param messageCode The code associated with the message, typically used for identifying
     *                    message types or categories.
     */
    public void sendWebSocketMessage(String message, Integer messageCode) {
        WebSocketMessageDto webSocketMessage = new WebSocketMessageDto(
                webSocketClient.getUsername(), Role.PROVIDER.name(), message, messageCode, false);
        webSocketMessage.setPublicKey(webSocketMessageService.getMyPublicKeyBase64());
        sendWebSocketMessage(webSocketMessage);
    }

    /**
     * Sends a WebSocket message using the webSocketClient, if it is initialized.
     *
     * @param socketMessageDto the WebSocketMessage to be sent
     */
    public void sendWebSocketMessage(WebSocketMessageDto socketMessageDto) {
        if (webSocketClient != null) {
            if (socketMessageDto.isMessageJson() && webSocketMessageService.isSessionConsumerPublicKeySet()) {
                // Encrypt the user message with AES the key and encapsulated the key
                try {
                    byte[] aesKey = webSocketMessageService.get32BytesAesKey();
                    byte[] encapsulatedKey = webSocketMessageService.getAesEncapsulatedKey();

                    byte[] encryptedMessage = webSocketMessageService.encryptMessage(socketMessageDto.getMessage().getBytes(StandardCharsets.UTF_8), aesKey);

                    ObjectNode jsonNode = JsonNodeFactory.instance.objectNode();
                    jsonNode.put("encryptedMessage", StringUtil.bytesToBase64(encryptedMessage));
                    jsonNode.put("encapsulatedKey", StringUtil.bytesToBase64(encapsulatedKey));
                    socketMessageDto.setMessage(jsonNode.toString());
                    socketMessageDto.setMessageJson(true);
                } catch (GeneralSecurityException e) {
                    throw new RuntimeException(e);
                }
            }
            webSocketClient.sendMessage(socketMessageDto);
        }
    }

    /* ********************** */
    /* * Reporting Services * */
    /* ********************** */
    public BenchmarkReport generateComputerBenchmarkReport(boolean quick) throws Exception {
        return computerService.generateComputerBenchmarkReport(loginWallet, quick);
    }

    /**
     * Saves a benchmark report by converting it into a data transfer object (DTO) and sending it to the
     * web service. If the user is not logged in or an error occurs during the process, a runtime
     * exception is thrown.
     *
     * @param report the {@code BenchmarkReport} object containing the benchmark data to be saved.
     *               This includes various benchmark results (memory, CPU, GPU, storage, network)
     *               along with metadata like elapsed time, date created, provider's wallet address,
     *               and associated computer UUID.
     * @throws RuntimeException if an error occurs during the saving process.
     */
    public void saveBenchmarkReport(BenchmarkReport report) throws RuntimeException {
        try {
            if (isLoggedIn()){
                BenchmarkReportDto reportDto = new BenchmarkReportDto();
                reportDto.setElapsedTime(report.getElapsedTime());
                reportDto.setDateCreated(report.getDateCreated());
                reportDto.setDescription(report.getDescription());
                reportDto.setWalletAddress(report.getProvider().getWalletAddress());
                reportDto.setComputerUuid(report.getComputer().getUuid());
                reportDto.setMemoryBenchmarkResults(report.getMemoryBenchmarkResults());
                reportDto.setCpuBenchmarkResults(report.getCpuBenchmarkResults());
                reportDto.setGpuBenchmarkResults(report.getGpuBenchmarkResults());
                reportDto.setStorageBenchmarkResults(report.getStorageBenchmarkResults());
                reportDto.setNetworkBenchmarkResults(report.getNetworkBenchmarkResults());
                reportDto.setOtherBenchmarkResults(report.getOtherBenchmarkResults());

                nextGpuWebService.saveBenchmarkReport(reportDto);
            } else {
                String errorMessage = "Cannot save Benchmark report. User is not logged-in.";
                log.error("Cannot save Benchmark report. User is not logged-in.");
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> generateComputerUsageReport() {
        return computerService.generateComputerUsageReport();
    }

    /**
     * Saves the detected computer hardware information after verifying the user's login status.
     * If the user is logged in, the method persists the computer details, updates a global property,
     * and initializes a WebSocket client for communication. If any step fails, appropriate exceptions
     * are thrown to indicate the failure.
     *
     * @param detectedHardware the {@code Computer} object containing the detected hardware details
     *                         to be saved.
     * @return the saved {@code Computer} object after successful persistence.
     * @throws RuntimeException if the user is not logged in, or if initializing the WebSocket client fails.
     */
    @Transactional
    public Computer saveDetectedHardware(Computer detectedHardware) throws RuntimeException {
        if(isLoggedIn()) {
            Computer saveComputer = computerService.saveComputer(detectedHardware, loginWallet);
            saveGlobalProperty(GlobalPropertyConfig.COMPUTER_UUID, saveComputer.getUuid(), "java.lang.String");
            GlobalProperty jwtProperty = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);

            if(!initializeWebSocketClient(saveComputer.getUuid(), jwtProperty.getValueReference())){
                String errorMessage = "Computer saved successfully, but failed to initialize WebSocket client";
                log.error("Computer saved successfully, but failed to initialize WebSocket client");
                webSocketMessageService.invalidateSessionKeyPair();
                throw new RuntimeException(errorMessage);
            }
            return saveComputer;
        } else {
            String errorMessage = "User is not logged-in.";
            log.error("User is not logged-in.");
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Detects the hardware configuration of a computer using the hardware utility.
     *
     * @return a {@code Computer} object representing the detected hardware configuration.
     */
    public Computer detectHardware(){
        return hardwareUtil.detectComputer();
    }

    public HardwareReport generateComputerHardwareReport(boolean asText) throws Exception {
        return computerService.generateComputerHardwareReport(loginWallet, asText);
    }

    private void addJwtHeader(Map<String, String> headers) {
        GlobalProperty property = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);
        String token = null;
        if (property != null) {
            token = property.getValueReference();
        }
        headers.put("Authorization", token);
    }

    public AnomalyReport generateAnomalyReport() {
        return null;
    }

    public BaseReport generateConsumerUsageReport() {
        // TODO: Return history of all sessions connected, the date/time from and to that they were connected for,
        //  average system usage per session as well as an aggregate summary
        // TODO: Attach the earnings info in compute units. Do NOT reveal the Consumer's info
        // see also @generateComputerUsageReport above
        return null;
    }

    // ========================================================
    // Application Settings Toggles
    // ========================================================

    @Transactional
    public void updateAppTheme(String theme) {
        // Notify the Theme change event
        analyticsService.captureEvent(PosthogEvent.THEME_SELECTED.name(), Map.of("theme", theme));

        saveGlobalProperty(GlobalPropertyConfig.APP_THEME, theme, "java.lang.String");
        log.info("App theme updated to: {}", theme);
    }

    @Transactional
    public void togglePrivateMode(boolean enabled) {
        saveGlobalProperty(GlobalPropertyConfig.IS_PRIVATE_MODE, String.valueOf(enabled), "java.lang.Boolean");
        log.info("Private Mode toggled to: {}", enabled);
    }

    @Transactional
    public void toggleAdvancedMode(boolean enabled) {
        // Notify the Advanced Mode change event
        analyticsService.captureEvent(PosthogEvent.MODE_SELECTED.name(), Map.of("mode", enabled ? "advanced" : "basic"));

        saveGlobalProperty(GlobalPropertyConfig.IS_ADVANCED_MODE, String.valueOf(enabled), "java.lang.Boolean");
        log.info("Advanced Mode toggled to: {}", enabled);
    }

    // ========================================================
    // Application Settings Toggles
    // ========================================================

    @Transactional
    public void updateIsOpenclawSetupCompleteProperty(boolean setupComplete) {
        saveGlobalProperty(GlobalPropertyConfig.IS_OPENCLAW_SETUP_COMPLETED, String.valueOf(setupComplete), "java.lang.Boolean");
        log.info("OpenClaw setup completion toggled to: {}", setupComplete);
    }

    @Transactional
    public void toggleOpenclawShortcut(boolean enabled) {
        saveGlobalProperty(GlobalPropertyConfig.SHOW_OPENCLAW_SHORTCUT, String.valueOf(enabled), "java.lang.Boolean");
        log.info("OpenClaw app navigation shortcut toggled to: {}", enabled);
    }
}
