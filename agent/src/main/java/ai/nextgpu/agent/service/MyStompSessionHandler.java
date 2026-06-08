package ai.nextgpu.agent.service;


import ai.nextgpu.common.dto.WebSocketMessageDto;
import ai.nextgpu.common.model.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;

import java.lang.reflect.Type;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This class is a custom implementation of {@link StompSessionHandlerAdapter} for managing
 * WebSocket STOMP session events and handling message communication. It oversees the session
 * lifecycle events such as connection establishment, subscription to topics, and error handling.
 * It also defines a custom message handler for receiving and processing incoming messages.
 */
@Slf4j
@Getter
@Setter
public class MyStompSessionHandler extends StompSessionHandlerAdapter {

    private String username;

    private MessageListener messageListener;

    public MyStompSessionHandler(MessageListener messageListener,String username) {
        this.username = username;
        this.messageListener = messageListener;
    }

    /**
     * Handles the event that occurs after a WebSocket STOMP connection has been successfully established.
     * It logs the connection details, sends a connection message to the server, subscribes to a specific
     * topic to receive messages, and sets up a frame handler to process incoming STOMP frames.
     *
     * @param session the established WebSocket STOMP session
     * @param connectedHeaders the headers received during the connection establishment
     */
    @Override
    public void afterConnected(StompSession session, @NotNull StompHeaders connectedHeaders) {
        log.info("Connected to WebSocket session {}", session.getSessionId());

        try {
            String publicKeyBase64 = Base64.getEncoder().encodeToString(messageListener.getMyPublicKey().getEncoded());
            session.send("/app/connect", new WebSocketMessageDto(username, publicKeyBase64, Role.PROVIDER.name()));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        // Subscribe to the messages topic to receive messages from the server
        session.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            public @NotNull Type getPayloadType(@NotNull StompHeaders headers) {
                return WebSocketMessageDto.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof WebSocketMessageDto message) {
                        messageListener.onMessageReceived(message);
                        log.info("Received message: {} from User {}", message.getMessage(), message.getLinkedEntityId());
                    } else {
                        log.error("Received invalid payload for `/topic/messages` of type {}", payload.getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("Error occurred while handling `/topic/messages` frame: {}", e.getMessage());
                }
            }
        });
        log.debug("Subscribed to topic /topic/messages for User {}", username);

        // Subscribe to the providers list topic
        session.subscribe("/topic/providers", new StompFrameHandler() {
            @Override
            public @NotNull Type getPayloadType(@NotNull StompHeaders headers) {
                return ArrayList.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof List<?> providers) {
                        log.info("Received provider list of size: {}", providers.size());
                    } else {
                        log.error("Received invalid payload for `/topic/providers` of type {}",
                                payload.getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("Error occurred while handling `/topic/providers` frame: {}", e.getMessage());
                }
            }
        });

        // Subscribe to user-specific queue for direct messages
        session.subscribe("/user/" + username + "/queue/messages", new StompFrameHandler() {
            @Override
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return WebSocketMessageDto.class;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                try {
                    if (payload instanceof WebSocketMessageDto message) {
                        messageListener.onMessageReceived(message);
                        log.info("Received direct message: {} from {}", message.getMessage(), message.getLinkedEntityId());
                    } else {
                        log.error("Received invalid payload for user queue of type {}",
                                payload.getClass().getName());
                    }
                } catch (Exception e) {
                    log.error("Error occurred while handling direct message: {}", e.getMessage());
                }
            }
        });

    }

    /**
     * Handles transport-level errors that occur during a WebSocket STOMP session.
     * Logs the error details and delegates the error handling to the parent implementation.
     *
     * @param session the current WebSocket STOMP session where the error occurred
     * @param exception the exception representing the transport error
     */
    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        log.error("Error occurred in WebSocket session {} : {}", session.getSessionId(), exception.getMessage());

        try {
            if (messageListener instanceof WebSocketMessageService webSocketMessageService) {
                webSocketMessageService.invalidateSessionKeyPair();
            }
        } catch (Exception e) {
            log.error("Failed to invalidate session keypair after transport error: {}", e.getMessage());
        }

        try {
            // Since you do not want to reconnect logic, close the session to stop cleanly.
            if (session.isConnected()) {
                session.disconnect();
            }
        } catch (Exception e) {
            log.error("Failed to disconnect STOMP session after transport error: {}", e.getMessage());
        }

        super.handleTransportError(session, exception);
    }
}
