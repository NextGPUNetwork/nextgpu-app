package ai.nextgpu.agent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.agent.aop.Loggable;
import ai.nextgpu.agent.model.ChatMessage;
import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.OSUtil;
import ai.nextgpu.common.ConsumerRequestCodes;
import ai.nextgpu.common.WebSocketCodes;
import ai.nextgpu.common.model.Role;
import ai.nextgpu.common.dto.WebSocketMessageDto;
import ai.nextgpu.common.report.HardwareReport;
import ai.nextgpu.common.util.JsonUtil;
import ai.nextgpu.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.SecretKeyWithEncapsulation;
import org.bouncycastle.pqc.jcajce.spec.KyberParameterSpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles inbound and outbound WebSocket/STOMP messages for the Agent (provider-side).
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Maintains a per-session post-quantum (Kyber) key pair used for key encapsulation.</li>
 *   <li>Stores the connected consumer's public key once received from the server.</li>
 *   <li>Decrypts incoming encrypted payloads and dispatches them by {@link WebSocketCodes}.</li>
 *   <li>Generates and sends heartbeat replies and report responses (hardware/benchmark/usage).</li>
 *   <li>Maintains a small in-memory chat history for conversational requests.</li>
 * </ul>
 * <p>
 * Session lifecycle:
 * <ol>
 *   <li>Call {@link #rotateSessionKeyPair()} before establishing the STOMP connection.</li>
 *   <li>Send {@link #getMyPublicKeyBase64()} to the server during connect so other peers can encapsulate to you.</li>
 *   <li>On disconnect/transport error, call {@link #invalidateSessionKeyPair()} and rotate again for a new session.</li>
 * </ol>
 * <p>
 * Thread-safety: key material is stored in {@link java.util.concurrent.atomic.AtomicReference AtomicReference}s to
 * allow safe updates across messaging callbacks.
 */
@Slf4j
@Service
public class WebSocketMessageService implements MessageListener {

    private NextGpuAgentService service;

    private final NextGpuAiService aiService;

    private final BenchmarkUtil benchmarkUtil;

    private final HardwareUtil hardwareUtil;

    private final OSUtil osUtil;

    private final ObjectMapper objectMapper;

    private final AgentSecurityService agentSecurityService;

    private final AtomicReference<KeyPair> sessionKeyPair = new AtomicReference<>();

    private final AtomicReference<String> sessionPublicKeyBase64 = new AtomicReference<>();

    private final AtomicReference<PublicKey> sessionConsumerPublicKey = new AtomicReference<>();

    private List<ChatMessage> messageHistory = new ArrayList<>();

    @Autowired
    public WebSocketMessageService(NextGpuAiService aiService, BenchmarkUtil benchmarkUtil, HardwareUtil hardwareUtil, OSUtil osUtil, NextGpuAgentService agentService, AgentSecurityService agentSecurityService) {
        this.aiService = aiService;
        this.benchmarkUtil = benchmarkUtil;
        this.hardwareUtil = hardwareUtil;
        this.osUtil = osUtil;
        this.service = agentService;
        this.agentSecurityService = agentSecurityService;
        this.objectMapper = JsonUtil.OBJECT_MAPPER;
    }

    @Autowired
    public void setNextGpuAgentService(NextGpuAgentService service) {
        this.service = service;
    }

    /**
     * Generates and stores a fresh Kyber key pair for the current WebSocket/STOMP session.
     * <p>
     * This should be invoked once per new session (or reconnection) to avoid reusing key material.
     *
     * @throws IllegalStateException if key generation fails
     */
    public void rotateSessionKeyPair() {
        try {
            KeyPair kp = agentSecurityService.generateKyberKeyPair(KyberParameterSpec.kyber1024);
            sessionKeyPair.set(kp);
            String pub64 = agentSecurityService.publicKeyToBase64(agentSecurityService.extractPublicKey(kp));
            sessionPublicKeyBase64.set(pub64);
            log.info("Rotated Kyber session keypair for new WebSocket/STOMP session");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate Kyber session keypair", e);
        }
    }

    /**
     * Returns the Base64-encoded public key for the current session.
     * <p>
     * The public key is intended to be sent to the server during the initial connect so the peer can
     * encapsulate a shared secret to this agent.
     *
     * @return Base64-encoded session public key (X.509/DER encoded bytes)
     * @throws IllegalStateException if {@link #rotateSessionKeyPair()} has not been called yet
     */
    public String getMyPublicKeyBase64() {
        String v = sessionPublicKeyBase64.get();
        if (v == null) {
            throw new IllegalStateException("Session keypair not initialized. Call rotateSessionKeyPair() before connect.");
        }
        return v;
    }

    /**
     * Clears all session-scoped key material (own key pair, published public key, and peer public key).
     * <p>
     * Call this when the transport disconnects or the session is considered finished.
     */
    public void invalidateSessionKeyPair() {
        sessionKeyPair.set(null);
        sessionPublicKeyBase64.set(null);
        sessionConsumerPublicKey.set(null);
        log.warn("Session keypair invalidated (transport disconnected or session ended)");
    }

    private PrivateKey getMyPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeyPair kp = sessionKeyPair.get();
        if (kp == null) {
            throw new IllegalStateException("Session keypair not initialized. Call rotateSessionKeyPair() before connect.");
        }
        return agentSecurityService.extractPrivateKey(kp);
    }

    @Override
    public PublicKey getMyPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        KeyPair kp = sessionKeyPair.get();
        if (kp == null) {
            throw new IllegalStateException("Session keypair not initialized. Call rotateSessionKeyPair() before connect.");
        }
        return agentSecurityService.extractPublicKey(kp);
    }

    /**
     * Derives a 32-byte AES key using Kyber key encapsulation with the currently stored consumer public key.
     * <p>
     * This performs an encapsulation operation and returns the first 32 bytes of the derived secret.
     * The corresponding encapsulated key bytes should be sent along with the encrypted message so the consumer
     * can decapsulate and derive the same secret.
     *
     * @return 32-byte AES key material
     * @throws IllegalStateException if the consumer public key has not been set yet
     */
    @Override
    public byte[] get32BytesAesKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        SecretKeyWithEncapsulation senderKey = agentSecurityService.generateEncryptionKey(sessionConsumerPublicKey.get());
        byte[] encryptionKey = senderKey.getEncoded();       // ephemeral secret
        // 3. Generate a 256-bit AES key to encrypt data
        return Arrays.copyOf(encryptionKey, 32);
    }

    /**
     * Checks if the session consumer public key has been set.
     *
     * @return true if the session consumer public key is not null, false otherwise.
     */
    public boolean isSessionConsumerPublicKeySet(){
        return sessionConsumerPublicKey.get() != null;
    }
    
    /**
     * Produces the Kyber encapsulated key bytes for the current encapsulation operation.
     * <p>
     * The encapsulated key must accompany the encrypted payload so the recipient can decapsulate.
     *
     * @return encapsulated key bytes (KEM encapsulation)
     * @throws IllegalStateException if the consumer public key has not been set yet
     */
    @Override
    public byte[] getAesEncapsulatedKey() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
        SecretKeyWithEncapsulation senderKey = agentSecurityService.generateEncryptionKey(sessionConsumerPublicKey.get());
        return senderKey.getEncapsulation();
    }

    @Override
    public byte[] encryptMessage(byte[] plaintext, byte[] aesKey) throws GeneralSecurityException {
        return agentSecurityService.encrypt(plaintext, aesKey);
    }

    /**
     * Receives a message from the server and dispatches it based on {@link WebSocketCodes}.
     * <p>
     * If the payload is a JSON object containing an encrypted envelope, the method attempts to:
     * <ol>
     *   <li>Read {@code encapsulatedKey} and {@code encryptedMessage} from the JSON.</li>
     *   <li>Decapsulate the shared secret using the agent's session private key.</li>
     *   <li>Decrypt the message bytes and interpret them as UTF-8 JSON or plain text.</li>
     * </ol>
     * After decryption, the message is handled according to its {@code messageCode}.
     * <p>
     * Notable message codes:
     * <ul>
     *   <li>{@link WebSocketCodes#HEARTBEAT}: replies with an "Alive" heartbeat.</li>
     *   <li>{@link WebSocketCodes#HARDWARE_REPORT}, {@link WebSocketCodes#BENCHMARK_REPORT},
     *       {@link WebSocketCodes#USAGE_REPORT}: generates the requested report and responds.</li>
     *   <li>{@link WebSocketCodes#CONSUMER_REQUEST}: processes a chat/command request and responds.</li>
     *   <li>{@link WebSocketCodes#CONNECT_WITH_CONSUMER}: stores the consumer public key for subsequent encryption.</li>
     * </ul>
     *
     * @param socketMessageDto inbound STOMP payload DTO from the server
     */
    @Override
    @Loggable
    public void onMessageReceived(WebSocketMessageDto socketMessageDto) {
        log.info("Received message: {} from User {}", socketMessageDto.getMessage(), socketMessageDto.getLinkedEntityId());

        String messageJsonString = "";
        if (StringUtil.isValidJson(socketMessageDto.getMessage()) &&
                Objects.equals(socketMessageDto.getUserRole(), Role.CONSUMER.name())) {
            try {
                JsonNode encryptedMessageJson = socketMessageDto.getJsonMessage();
                String encapsulatedKey = encryptedMessageJson.get("encapsulatedKey").asText();
                String encryptedMessage = encryptedMessageJson.get("encryptedMessage").asText();
                // Decrypt the encapsulated key using the private key
                byte[] sharedSecret = agentSecurityService.generateDecryptionKey(getMyPrivateKey(), StringUtil.base64ToBytes(encapsulatedKey));

                // Decrypt the encrypted message using the shared (AES) secret
                byte[] decrypted = agentSecurityService.decrypt(
                        sharedSecret,
                        StringUtil.base64ToBytes(encryptedMessage)
                );

                messageJsonString = new String(decrypted, StandardCharsets.UTF_8);

            } catch (GeneralSecurityException e) {
                log.error("Error decrypting message: {}", e.getMessage());
                return;
            }
        } else {
            messageJsonString = socketMessageDto.getMessage();
        }

        // TODO: Send acknowledgement to the server

        WebSocketCodes messageCode = WebSocketCodes.fromValue(socketMessageDto.getMessageCode());
        switch (messageCode) {
            case READY_TO_SERVE, UNABLE_TO_SERVE, CONNECT_WITH_PROVIDER, DISCONNECT_FROM_PROVIDER,
                 PROVIDER_RESPONSE -> {
                log.info("Bad request. Agent cannot process request with code: {}({})", messageCode.name(), messageCode.getValue());
            }
            case CONNECTION_FAILED -> {
                // TODO Handle connection failure
            }
            case PROVIDER_REQUEST -> {
                // The server will send a message with message code of PROVIDER_REQUEST, if there is no Consumer Connected.
                // Role in the message must be ADMIN
                // TODO Handle the situation in case there is no Consumer Connected
            }
            case HEARTBEAT -> {
                log.info("Heartbeat received from {}", socketMessageDto.getLinkedEntityId());
                WebSocketMessageDto response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), "Alive", WebSocketCodes.HEARTBEAT.getValue());
                service.sendWebSocketMessage(response);
            }
            case HARDWARE_REPORT -> {
                log.info("Hardware report request received from {}", socketMessageDto.getLinkedEntityId());
                HardwareReport report = null;
                try {
                    report = service.generateComputerHardwareReport(false);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                WebSocketMessageDto response = null;
                try {
                    response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), objectMapper.writeValueAsString(report.asJson()), WebSocketCodes.HARDWARE_REPORT.getValue(), true);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                service.sendWebSocketMessage(response);
                //TODO: Check the completeness of feature
            }
            case BENCHMARK_REPORT -> {
                log.info("Benchmark report received from {}", socketMessageDto.getLinkedEntityId());
                WebSocketMessageDto response = null;
                try {
                    response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), objectMapper.writeValueAsString(service.generateComputerBenchmarkReport(true)), WebSocketCodes.BENCHMARK_REPORT.getValue(), true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                service.sendWebSocketMessage(response);
                //TODO: Check the completeness of feature
            }
            case USAGE_REPORT -> {
                log.info("Usage report received from {}", socketMessageDto.getLinkedEntityId());
                // TODO the usage report should be saved into database ???
                Map<String, Object> usage = service.generateComputerUsageReport();

                WebSocketMessageDto response = null;
                try {
                    response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), objectMapper.writeValueAsString(usage), WebSocketCodes.USAGE_REPORT.getValue(), true);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                service.sendWebSocketMessage(response);
                //TODO: Check the completeness of feature
            }
            case CONSUMER_REQUEST -> {
                log.info("Consumer request received from {}", socketMessageDto.getLinkedEntityId());
                String responseMessage = "Dear Server, I have received " + messageJsonString + " from " + socketMessageDto.getLinkedEntityId() + " which I'm forwarding to you.";
                if (StringUtil.isValidJson(messageJsonString)) {
                    try {
                        JsonNode jsonNode = socketMessageDto.getJsonMessage(messageJsonString);

                        log.info("Parsed JSON message: {}", jsonNode);
                        String model = jsonNode.has("model") ? jsonNode.get("model").asText() : "default-model";
                        String context = jsonNode.get("context").asText();
                        String history = jsonNode.get("history").asText();  //TODO: cold history attachment feature pending
                        String prompt = jsonNode.get("prompt").asText();

                        // Context should be added only once
                        if (context != null && !context.isBlank() && messageHistory.size() == 0) {
                            updateHistory(new ChatMessage("system", context), true);
                        }

                        // Add new prompt to history
                        updateHistory(new ChatMessage("user", prompt), false);

                        // Generate response
                        responseMessage = aiService.generateResponseWithHistory(model, messageHistory, prompt);

                        // Add LLM's response to history
                        updateHistory(new ChatMessage("assistant", responseMessage), false);
                    } catch (Exception e) {
                        log.error("Error parsing JSON message: {}", e.getMessage());
                        responseMessage = "Error parsing JSON message: " + e.getMessage();
                    }
                } else {
                    try {
                        // TODO add something to differentiate chat response and command response
                        // Coordinate with UI Devs to change the current handling
                        responseMessage = "COMMAND OUTPUT:" + OSUtil.executeCommand(socketMessageDto.getMessage());
                    } catch (Exception e) { //TODO handle error
                        responseMessage = "COMMAND OUTPUT:" + "Error executing command: " + e.getMessage();
                    }
                }
                WebSocketMessageDto response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), responseMessage, WebSocketCodes.PROVIDER_RESPONSE.getValue(), false);
                service.sendWebSocketMessage(response);
            }
            // Sets the Consumer's public key, received from the system upon connection
            case CONNECT_WITH_CONSUMER -> {
                String consumerPublicKeyBase64 = socketMessageDto.getPublicKey();
                try {
                    sessionConsumerPublicKey.set(agentSecurityService.publicKeyFromBase64(consumerPublicKeyBase64));
                } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
                    throw new RuntimeException(e);
                }
            }
            case ACKNOWLEDGEMENT_FAILED -> {
                // TODO Handle acknowledgement failure to show on the UI
                log.warn("Acknowledgement failed for message: {}", socketMessageDto.getMessage());
            }
            case null -> {
                log.warn("Unknown message code received from {}", socketMessageDto.getLinkedEntityId());
            }
        }
    }

    private void handleConsumerRequest(WebSocketMessageDto message) {
        WebSocketMessageDto response = null;

        String responseMessage = "Dear Server, I have received " + message.getMessage() + " from " + message.getLinkedEntityId() + " which I'm forwarding to you.";
        if (StringUtil.isValidJson(message.getMessage())) {
            JsonNode jsonNode = message.getJsonMessage();
            log.info("Parsed JSON message: {}", jsonNode);

            ConsumerRequestCodes requestCode = ConsumerRequestCodes.fromValue(jsonNode.get("requestCode").asInt());
            if (requestCode == null) {
                log.error("Invalid request code received: {}", jsonNode.get("requestCode").asInt());
                responseMessage = "Invalid request code received: " + jsonNode.get("requestCode").asInt();
            } else if (requestCode == ConsumerRequestCodes.CHAT_QUERY) {
                responseMessage = generateChatResponse(jsonNode);
            } else if (requestCode == ConsumerRequestCodes.SYSTEM_COMMAND) {
                responseMessage = generateCommandResponse(jsonNode);
            }
            response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), responseMessage, WebSocketCodes.PROVIDER_RESPONSE.getValue(), requestCode.getValue(), false);
        } else {
            response = new WebSocketMessageDto(service.getLoginWallet(), Role.PROVIDER.name(), responseMessage, WebSocketCodes.PROVIDER_RESPONSE.getValue());
        }
        service.sendWebSocketMessage(response);
    }

    private String generateChatResponse(JsonNode jsonNode) {
        String responseMessage = null;
        try {
            String model = jsonNode.has("model") ? jsonNode.get("model").asText() : "default-model";
            String context = jsonNode.get("context").asText();
            String history = jsonNode.get("history").asText();  //TODO: cold history attachment feature pending
            String prompt = jsonNode.get("prompt").asText();

            // Context should be added only once
            if (context != null && !context.isBlank() && messageHistory.size() == 0) {
                updateHistory(new ChatMessage("system", context), true);
            }

            // Add new prompt to history
            updateHistory(new ChatMessage("user", prompt), false);

            // Generate response
            responseMessage = aiService.generateResponseWithHistory(model, messageHistory, prompt);

            // Add LLM's response to history
            updateHistory(new ChatMessage("assistant", responseMessage), false);
        } catch (Exception e) {
            log.error("Error parsing JSON message: {}", e.getMessage());
            responseMessage = "Error parsing CHAT JSON message: " + e.getMessage();
        }
        return responseMessage;
    }

    private String generateCommandResponse(JsonNode jsonNode) {
        String prompt = jsonNode.get("prompt").asText();

        String responseMessage = null;
        try {
            // Coordinate with UI Devs to change the current handling
            responseMessage = OSUtil.executeCommand(prompt);
        } catch (Exception e) { //TODO handle error
            responseMessage = "Error executing command: " + e.getMessage();
        }
        return responseMessage;
    }

    @Override
    public void onActiveUsersUpdated(ArrayList<String> users) {
        log.info("Active users updated: {}", String.join(", ", users));
    }

    /**
     * Updates the in-memory chat history used for conversational requests.
     * <p>
     * If {@code reset} is true, the history is cleared before adding {@code newMessage}.
     * A small rolling window is maintained to limit prompt growth; the oldest non-system entry is pruned first
     * (the initial system/context message is kept when present).
     *
     * @param newMessage message to append
     * @param reset      whether to clear existing history before appending
     */
    public void updateHistory(ChatMessage newMessage, boolean reset) {
        if (reset) {
            messageHistory = new ArrayList<>();
        }
        // Prune
        if (messageHistory.size() > 5) {
            messageHistory.remove(1);   // Remember! First one contains the context
        }
        messageHistory.add(newMessage);
    }
}
