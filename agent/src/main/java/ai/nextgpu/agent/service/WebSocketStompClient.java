package ai.nextgpu.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ai.nextgpu.common.dto.WebSocketMessageDto;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Getter
@Setter
public class WebSocketStompClient {

    private static final Logger log = LoggerFactory.getLogger(WebSocketStompClient.class);

    StompSession session;

    String username;

    /**
     * Constructs a new instance of the WebSocketStompClient to interact with a WebSocket server using STOMP (Simple Text Oriented Messaging Protocol)
     * over SockJS or raw WebSocket connections. This client features automatic connection handling, authentication using JWT tokens,
     * and message exchange. Subscriptions to specific topics are managed, and received messages are handled via the provided MessageListener.
     * Note to Devs! This method is the key to understanding how the communication between the Agent and the Server works via Web Sockets
     *
     * @param messageListener an implementation of MessageListener to handle incoming messages and active user updates
     * @param username the username of the client, used for identification within the WebSocket session
     * @param url the WebSocket server URL, which is converted to use SockJS or remains as a raw WebSocket URL
     * @param jwtToken the JWT token for authentication, if required
     * @throws ExecutionException if the WebSocket connection fails and all reconnection attempts also fail
     */
    public WebSocketStompClient(MessageListener messageListener, String username, String url, String jwtToken) throws ExecutionException {

        // Store the username as a class field for later identification in the session
        this.username = username;
        log.info("Starting WebSocket connection with URL: {}", url);

        // Create connection headers with a JWT token
        StompHeaders connectHeaders = new StompHeaders();
        if (jwtToken != null && !jwtToken.isEmpty()) {
            connectHeaders.add("Authorization", "Bearer " + jwtToken);
            log.debug("Added JWT token to connection headers");
        }

        // Also set login from username if needed
        connectHeaders.setLogin(username);

        // First attempt: Try direct WebSocket connection
        // This is the preferred method as it's more efficient than SockJS
        try {
            // Initialize a standard WebSocket client that implements WebSocket protocol
            WebSocketClient webSocketClient = new StandardWebSocketClient();

            // Create a STOMP client wrapper around the WebSocket client
            // This allows us to use STOMP (Simple Text Oriented Messaging Protocol) over WebSocket
            org.springframework.web.socket.messaging.WebSocketStompClient stompClient =
                    new org.springframework.web.socket.messaging.WebSocketStompClient(webSocketClient);

            // Set up Jackson converter to handle JSON message conversion
            // This enables automatic conversion between Java objects and JSON messages
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());

            // Convert HTTP/HTTPS URLs to their WebSocket equivalents (ws:// or wss://)
            // WebSocket protocol requires ws:// or wss:// instead of http:// or https://
            String wsUrl = url;
            if (url.startsWith("http://")) {
                wsUrl = url.replace("http://", "ws://");
            } else if (url.startsWith("https://")) {
                wsUrl = url.replace("https://", "wss://");
            }
            // Create a session handler that will manage the STOMP session
            // This handler processes incoming messages and connection status updates
            log.info("Attempting to connect to: {}", wsUrl);
            MyStompSessionHandler sessionHandler = new MyStompSessionHandler(messageListener, username);

            // Establish the STOMP session asynchronously and wait for completion
            // The .get() call blocks until the connection is established or fails
            session = stompClient.connectAsync(wsUrl, sessionHandler, connectHeaders).get();

        } catch (Exception e) {
            // If the direct WebSocket connection fails, log the error and try SockJS fallback
            log.error("Direct WebSocket connection failed: {}", e.getMessage());

            // Second attempt: Try SockJS fallback
            // SockJS provides additional fallback transports when WebSocket isn't available
            try {
                log.info("Trying SockJS fallback...");

                // Convert WebSocket URLs back to HTTP/HTTPS, for SockJS
                //  requires HTTP/HTTPS URLs as it handles the protocol upgrade internally
                String sockJsUrl = url;
                if (url.startsWith("ws://")) {
                    sockJsUrl = url.replace("ws://", "http://");
                } else if (url.startsWith("wss://")) {
                    sockJsUrl = url.replace("wss://", "https://");
                }
                // Configure SockJS client with WebSocket transport
                // This sets up the available transport options for SockJS
                List<Transport> transports = new ArrayList<>();
                transports.add(new WebSocketTransport(new StandardWebSocketClient()));
                SockJsClient sockJsClient = new SockJsClient(transports);

                // Create a new STOMP client using SockJS instead of raw WebSocket
                org.springframework.web.socket.messaging.WebSocketStompClient stompClient =
                        new org.springframework.web.socket.messaging.WebSocketStompClient(sockJsClient);
                stompClient.setMessageConverter(new MappingJackson2MessageConverter());

                // Create a new session handler and attempt to connect using SockJS
                log.info("Connecting to SockJS at: {}", sockJsUrl);
                MyStompSessionHandler sessionHandler = new MyStompSessionHandler(messageListener, username);

                // Establish the STOMP session using SockJS and wait for completion
                session = stompClient.connectAsync(sockJsUrl, sessionHandler, connectHeaders).get();
            } catch (Exception e2) {
                // If both connection attempts fail, throw an exception
                log.error("SockJS fallback also failed: {}", e2.getMessage());
                throw new ExecutionException("All WebSocket connection attempts failed", e2);
            }
        }
    }

    /**
     * Sends a message to a predefined destination using the WebSocket STOMP session.
     * Logs the success or any error that occurs during the message sending process.
     *
     * @param message the message object containing the message content and user information to be sent
     */
    public void sendMessage(WebSocketMessageDto message) {
        try {
            log.info("Sending message from: {} - {}", message.getLinkedEntityId(), message.getMessage());
            if (message.isMessageJson()) {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonMessage = objectMapper.writeValueAsString(message);
                session.send("/app/message", jsonMessage);
            } else {
                session.send("/app/message", message);
            }
        } catch (Exception e) {
            log.error("Error occurred while sending message: {}", e.getMessage());
        }
    }


    /**
     * Disconnects the user from the WebSocket STOMP session.
     * Sends a disconnection message to the server and logs the operation.
     *
     * @param username the username of the client to be disconnected
     */
    public void disconnect(String username) {
        try {
            session.send("/app/disconnect", username);
            log.info("Disconnected user {}", username);
        } catch (Exception e) {
            log.error("Error occurred while disconnecting user: {}", e.getMessage());
        }
    }
}
