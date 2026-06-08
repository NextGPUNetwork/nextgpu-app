package ai.nextgpu.common;

public enum WebSocketMessageState {
    PENDING,
    SENT,
    ACKNOWLEDGED,
    FAILED,
    RETRY, // The message state when a message is sent and is not acknowledged within a certain time, then resent again
    EXPIRED
}
