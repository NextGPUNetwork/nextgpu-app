package ai.nextgpu.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import ai.nextgpu.common.util.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessageDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String messageId = UUID.randomUUID().toString();

    private String linkedEntityId;

    private String message;

    private String userRole;

    private Integer messageCode;

    private String publicKey;

    private Integer requestCode;

    private boolean isMessageJson = false;

    private String messageTime = String.valueOf(System.currentTimeMillis());

    public WebSocketMessageDto(String linkedEntityId, String publicKey) {
        this.linkedEntityId = linkedEntityId;
        this.publicKey = publicKey;
    }

    public WebSocketMessageDto(String linkedEntityId, String publicKey, String userRole) {
        this.linkedEntityId = linkedEntityId;
        this.publicKey = publicKey;
        this.userRole = userRole;
    }

    public WebSocketMessageDto(String linkedEntityId, String userRole, String message, Integer messageCode) {
        this(linkedEntityId, userRole, message, messageCode, false);
    }

    public WebSocketMessageDto(String linkedEntityId, String userRole, String message, Integer messageCode, boolean isMessageJson) {
        this(linkedEntityId, userRole, message, messageCode, null, isMessageJson);
    }

    public WebSocketMessageDto(String linkedEntityId, String userRole, String message, Integer messageCode, Integer requestCode, boolean isMessageJson) {
        this.linkedEntityId = linkedEntityId;
        this.message = message;
        this.userRole = userRole;
        this.messageCode = messageCode;
        this.isMessageJson = isMessageJson;
        this.requestCode = requestCode;
    }

    public JsonNode getJsonMessage(String message) {
        try {
            return JsonUtil.OBJECT_MAPPER.readTree(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public JsonNode getJsonMessage() {
        if (!isMessageJson || message == null || message.isBlank()) {
            return NullNode.getInstance();
        }
        try {
            return JsonUtil.OBJECT_MAPPER.readTree(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        WebSocketMessageDto that = (WebSocketMessageDto) o;
        return Objects.equals(messageId, that.messageId) && Objects.equals(linkedEntityId, that.linkedEntityId) && Objects.equals(userRole, that.userRole) && Objects.equals(message, that.message) && Objects.equals(messageTime, that.messageTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, linkedEntityId, userRole, message, messageTime);
    }
}
