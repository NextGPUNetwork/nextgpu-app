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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.function.Consumer;

/**
 * Service class responsible for handling operations related to GPU agents, WebSocket communication,
 * authentication, reporting, and other ancillary services. It serves as the central point of interaction
 * for GPU-related functionalities and provides methods for managing global properties, user authentication,
 * WebSocket communication, hardware detection, and resource recommendations.
 */
@Getter
@Service
public class NextGpuAgentService {

    private static final Logger log = LoggerFactory.getLogger(NextGpuAgentService.class);

    // TODO: Remove this field and use the hasValidJwt() method instead
    private boolean isLoggedIn = false;

    // TODO: Remove this field and use the LOGIN_WALLET property instead
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
    private final ProviderService providerService;

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
            ComputerService computerService, AnalyticsService analyticsService, ProviderService providerService) {
        this.globalPropertyRepository = globalPropertyRepository;
        this.dataService = dataService;
        this.webSocketMessageService = webSocketMessageService;
        this.hardwareUtil = hardwareUtil;
        this.computerService = computerService;
        this.analyticsService = analyticsService;
        this.providerService = providerService;
        checkEnvironment();
    }

    /**
     * Verifies if WSL is running.
     */
    public void checkEnvironment() {
        if (OSUtil.IS_WINDOWS) {
            GlobalProperty setupComplete = getGlobalProperty(GlobalPropertyConfig.IS_SETUP_COMPLETED);
            if (setupComplete != null && setupComplete.getValue() == Boolean.TRUE) {
                // WSL startup is now handled asynchronously in NextGpuAgentApplication to avoid blocking Spring startup
                log.info("WSL startup will be handled by the application UI flow.");
            }
            // Identify WSL IP address and store in respective global property
            GlobalProperty distroProperty = getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO);
            GlobalProperty usernameProperty = getGlobalProperty(GlobalPropertyConfig.OS_USERNAME);
            GlobalProperty localIpProperty = getGlobalProperty(GlobalPropertyConfig.LOCAL_IP);

            // Only proceed if all required properties exist
            if (distroProperty != null && usernameProperty != null) {
                String distro = distroProperty.getValueReference();
                String username = usernameProperty.getValueReference();
                if (distro != null && username != null) {
                    String wslIp = OSUtil.getLocalIpAddress(distro, username);
                    if (wslIp != null && !wslIp.isBlank()) {
                        localIpProperty.setValueReference(wslIp);
                        saveGlobalProperty(localIpProperty);
                    } else {
                        log.warn("Keeping existing WSL_IP value because local IP lookup failed for distro '{}'.", distro);
                    }
                }
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
     */
    public void saveGlobalProperty(String name, String value, String datatype) {
        Optional<GlobalProperty> globalProperty = globalPropertyRepository.findByName(name);
        if (globalProperty.isPresent()) {
            globalProperty.get().setValueReference(value);
            globalPropertyRepository.save(globalProperty.get());
        } else {
            GlobalProperty newGlobalProperty = new GlobalProperty();
            newGlobalProperty.setName(name);
            newGlobalProperty.setDatatype(datatype);
            newGlobalProperty.setValueReference(value);
            globalPropertyRepository.save(newGlobalProperty);
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


    @Transactional
    @Loggable
    public void saveSetupCompletionGlobalProperties(boolean isSetupCompleted, int setupVersion) {
        GlobalProperty setupCompletionProp = getGlobalProperty(GlobalPropertyConfig.IS_SETUP_COMPLETED);
        setupCompletionProp.setValueReference(String.valueOf(isSetupCompleted));
        saveGlobalProperty(setupCompletionProp);

        GlobalProperty setupVersionProp = getGlobalProperty(GlobalPropertyConfig.SETUP_VERSION);
        setupVersionProp.setValueReference(String.valueOf(setupVersion));
        saveGlobalProperty(setupVersionProp);
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
     * @param oneTimeKey    a one-time key associated with the user's session or authentication attempt
     * @return true if authentication is successful, false otherwise
     */
    @Transactional
    public boolean authenticate(String oneTimeKey) {
        try {
            AuthResponseDto response = nextGpuWebService.verifyOtp(oneTimeKey);
            log.debug("Authentication successful. JWT token: {}", response.getUserDto().getUuid());
            UserDto responseUserDto = response.getUserDto();
            if (!responseUserDto.getRole().equals(Role.PROVIDER)) {
                return false;
            }

            saveGlobalProperty(GlobalPropertyConfig.JWT_TOKEN, response.getAccessToken(), "java.lang.String");
            // Fetch computer attribute types and provider attribute types
            List<ComputerAttributeTypeDto> computerAttributeTypes = nextGpuWebService.getComputerAttributeTypes();
            log.debug("Fetched computer attribute types.");
            List<ProviderAttributeTypeDto> providerAttributeTypes = nextGpuWebService.getProviderAttributeTypes();
            log.debug("Fetched provider attribute types.");

            providerService.saveProviderAttributeTypes(providerAttributeTypes);

            Provider provider = providerService.saveProvider(responseUserDto);

            saveGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET, provider.getWalletAddress(), "java.lang.String");

            computerService.saveComputerAttributeTypes(computerAttributeTypes);
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
        GlobalProperty loginWalletProperty = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);
        return computerService.generateComputerBenchmarkReport(loginWalletProperty.getValueReference(), quick);
    }


    public Map<String, Object> generateComputerUsageReport() {
        return computerService.generateComputerUsageReport();
    }

    @Transactional
    public boolean applyAsAProvider(Consumer<String> workflowStatus){
        log.info("Applying as a Provider");
        try {
            if(hasValidJwt()){
                GlobalProperty loginWalletProperty = globalPropertyRepository.findByName(GlobalPropertyConfig.LOGIN_WALLET).get();
                String loginWallet = loginWalletProperty.getValueReference();
                log.info("User is logged in");
                log.info("Detecting hardware");

                workflowStatus.accept("Detecting provider hardware");
                Computer computer = this.detectHardware();

                log.info("Detected hardware.");

                computer = computerService.saveComputer(computer, loginWallet);
                log.info("Computer saved successfully: {}", computer.getUuid());

                // Generate a full benchmark report, not a quick one
                workflowStatus.accept("Generating hardware benchmark report");
                BenchmarkReport benchmarkReport= this.generateComputerBenchmarkReport(true);
                benchmarkReport.setComputer(computer);
                log.info("Benchmark report generated successfully.");

                nextGpuWebService.saveBenchmarkReport(BenchmarkReportDto.toDto(benchmarkReport));
                log.info("Benchmark report saved successfully: {}", benchmarkReport.getUuid());

                workflowStatus.accept("Auditing computer for anomalies");
                boolean auditResult = nextGpuWebService.auditComputerForAnomalies(ComputerDto.toDto(computer));
                log.info("Computer audit result: {}", auditResult);
                if (auditResult) {
                    fetchAndUpdateComputer(computer.getUuid());
                    log.info("Computer updated successfully: {}", computer.getUuid());
                }

                return auditResult;
            } else {
                String errorMessage = "Cannot apply as a provider. User is not logged-in.";
                log.error("Cannot apply as a provider. User is not logged-in.");
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
             throw new RuntimeException(e);
        }
    }


    // TODO: Remove this method after refactoring when working on WebSocket
    @Deprecated
    public Computer saveDetectedHardware(Computer detectedHardware) throws Exception {
        if(hasValidJwt()) {
            GlobalProperty loginWalletProperty = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);

            Computer saveComputer = computerService.saveComputer(detectedHardware, loginWalletProperty.getValueReference());
            saveGlobalProperty(GlobalPropertyConfig.COMPUTER_UUID, saveComputer.getUuid(), "java.lang.String");

            // TODO: Work on Work socket
//            GlobalProperty jwtProperty = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);
//            if(!initializeWebSocketClient(saveComputer.getUuid(), jwtProperty.getValueReference())){
//                String errorMessage = "Computer saved successfully, but failed to initialize WebSocket client";
//                log.error("Computer saved successfully, but failed to initialize WebSocket client");
//                webSocketMessageService.invalidateSessionKeyPair();
//                throw new RuntimeException(errorMessage);
//            }
            return saveComputer;
        } else {
            String errorMessage = "User is not logged-in.";
            log.error("User is not logged-in.");
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Fetches a computer's information using its UUID from an external service
     * and updates the corresponding computer entity in the system.
     *
     * @param computerUuid the unique identifier of the computer to fetch and update
     * @return the updated computer entity after applying the fetched information
     * @throws RuntimeException if an error occurs during the fetch or update process
     */
    @Transactional
    public Computer fetchAndUpdateComputer(String computerUuid) {
        try {
            ComputerDto computerDto = nextGpuWebService.getComputer(computerUuid);
            return computerService.updateComputer(computerDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        GlobalProperty loginWalletProperty = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);
        return computerService.generateComputerHardwareReport(loginWalletProperty.getValueReference(), asText);
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

    /**
     * Updates the selected installation profile (e.g., 'provider' or 'ai_hub').
     *
     * @param profile the installation profile to save
     */
    @Transactional
    public void updateInstallProfile(String profile) {
        saveGlobalProperty(GlobalPropertyConfig.INSTALL_PROFILE, profile, "java.lang.String");
        log.info("Installation profile updated to: {}", profile);
    }

    // ========================================================
    // Navigation State Management
    // ========================================================

    /**
     * Updates the last active screen to persist navigation state.
     *
     * @param screen the name of the active screen
     */
    @Transactional
    public void updateLastActiveScreen(String screen) {
        saveGlobalProperty(GlobalPropertyConfig.LAST_ACTIVE_SCREEN, screen, "java.lang.String");
        log.debug("Last active screen updated to: {}", screen);
    }

    /**
     * Retrieves the last active screen.
     *
     * @return the last active screen name, or an empty string if not set
     */
    public String getLastActiveScreen() {
        GlobalProperty prop = getGlobalProperty(GlobalPropertyConfig.LAST_ACTIVE_SCREEN);
        return prop != null && prop.getValueReference() != null ? prop.getValueReference() : "";
    }

    // ========================================================
    // Provider State Management
    // ========================================================

    /**
     * Checks if the detected hardware contains an NVIDIA GPU and saves the status.
     * @return true if an Nvidia GPU is found, false otherwise.
     */
    @Loggable
    @Transactional
    public boolean checkAndSaveNvidiaGpuPresence() {
        log.info("Checking for NVIDIA GPU presence...");

        Set<Gpu> gpus = hardwareUtil.detectGpus();

        boolean hasNvidia = false;
        if(log.isDebugEnabled()){
            log.warn("ai.nextgpu DEBUG logging enabled — skipping NVIDIA GPU detection");
            hasNvidia = true;
        } else {
            if (gpus != null) {
                hasNvidia = gpus.stream()
                        .anyMatch(gpu -> gpu.getManufacturer() != null
                                && gpu.getManufacturer().toLowerCase().contains("nvidia"));
            }
            log.info("NVIDIA GPU detected: {}", hasNvidia);
        }
        saveGlobalProperty(GlobalPropertyConfig.HAS_NVIDIA_GPU, String.valueOf(hasNvidia), "java.lang.Boolean");
        return hasNvidia;
    }

    // ========================================================
    // Local Entity Fetchers for ViewModel Hydration
    // ========================================================

    /**
     * Retrieves the complete locally stored Provider entity based on the current LOGIN_WALLET.
     */
    @Transactional(readOnly = true)
    public Provider getLocalProvider() {
        GlobalProperty walletProp = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);
        if (walletProp != null && walletProp.getValueReference() != null && !walletProp.getValueReference().isBlank()) {
            Provider provider = providerService.getProviderByWalletAddress(walletProp.getValueReference());
            if (provider != null && provider.getProviderAttributes() != null) {
                // Force initialization of the lazy collection while the transaction is active
                provider.getProviderAttributes().size();
            }
            return provider;
        }
        return null;
    }

    /**
     * Retrieves the complete locally stored Computer entity based on the saved COMPUTER_UUID.
     */
    @Transactional(readOnly = true)
    public Computer getLocalComputer() {
        GlobalProperty uuidProp = getGlobalProperty(GlobalPropertyConfig.COMPUTER_UUID);
        if (uuidProp != null && uuidProp.getValueReference() != null && !uuidProp.getValueReference().isBlank()) {
            Computer computer = dataService.findComputerByUuid(uuidProp.getValueReference()).orElse(null);
            if (computer != null && computer.getComputerAttributes() != null) {
                // Force initialization of the lazy collection while the transaction is active
                computer.getComputerAttributes().size();
            }
            return computer;
        }
        return null;
    }


    // ========================================================
    // JWT Validation Helpers
    // ========================================================

    /**
     * Decodes the locally stored JWT to verify if it is structurally valid and not expired.
     * It also verifies if the subject matches the provided wallet address.
     *
     * @param jwtToken The raw JWT token string
     * @param walletAddress The expected wallet address (subject)
     * @return true if valid and not expired, false otherwise
     */
    public boolean isJwtValid(String jwtToken, String walletAddress) {
        if (jwtToken == null || jwtToken.isBlank() || walletAddress == null || walletAddress.isBlank()) {
            return false;
        }

        try {
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            // Decode payload using Base64 URL decoder
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);

            // Check Expiration
            Object expObj = payloadMap.get("exp");
            if (expObj == null) return false;

            // Handle cases where Jackson parses the timestamp as Integer or Long
            long exp = (expObj instanceof Number) ? ((Number) expObj).longValue() : Long.parseLong(expObj.toString());
            boolean isExpired = LocalDateTime.now(ZoneOffset.UTC).isAfter(LocalDateTime.ofEpochSecond(exp, 0, ZoneOffset.UTC));

            if (isExpired) return false;

            // Verify Subject
            Object subObj = payloadMap.get("sub");
            if (subObj == null || !walletAddress.equals(subObj.toString())) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Failed to decode or validate JWT locally: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Convenience method that reads the current properties from the database and evaluates them.
     * * @return true if the stored token exists, is unexpired, and matches the stored wallet.
     */
    public boolean hasValidJwt() {
        GlobalProperty jwtProp = getGlobalProperty(GlobalPropertyConfig.JWT_TOKEN);
        GlobalProperty walletProp = getGlobalProperty(GlobalPropertyConfig.LOGIN_WALLET);

        String token = (jwtProp != null) ? jwtProp.getValueReference() : null;
        String wallet = (walletProp != null) ? walletProp.getValueReference() : null;

        return isJwtValid(token, wallet);
    }

//
//    @Transactional
//    public void updateProviderStakeStatus(boolean hasSufficientStake) {
//        saveGlobalProperty(GlobalPropertyConfig.PROVIDER_STAKE_STATUS, String.valueOf(hasSufficientStake), "java.lang.Boolean");
//        log.debug("Provider stake status updated to: {}", hasSufficientStake);
//    }
//
//    @Transactional
//    public void updateProviderAuditStatus(String auditStatus) {
//        saveGlobalProperty(GlobalPropertyConfig.PROVIDER_AUDIT_STATUS, auditStatus, "java.lang.String");
//        log.debug("Provider audit status updated to: {}", auditStatus);
//    }
}
