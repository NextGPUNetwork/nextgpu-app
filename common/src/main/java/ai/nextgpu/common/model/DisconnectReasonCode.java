package ai.nextgpu.common.model;

public enum DisconnectReasonCode {
    /**
     * Indicates that the consumer initiated the disconnection.
     * This typically occurs when a consumer intentionally terminates the connection.
     */
    CONSUMER_INITIATED,
    /**
     * Indicates that the disconnection occurred because the consumer of the service
     * has gone offline or is unavailable.
     */
    CONSUMER_OFFLINE,
    /**
     * Represents the state where the provider has gone offline, leading to a disconnection.
     * This is one of the possible reasons for a disconnect event in the system.
     */
    PROVIDER_OFFLINE,
    /**
     * Represents a disconnection reason caused by an administrative action.
     * This can be used to signify that the connection was intentionally
     * terminated by administrative control or oversight.
     */
    ADMIN_ACTION,
    /**
     * Represents an error state in the disconnection process.
     * Typically used when an unspecified or unexpected issue occurs
     * during a disconnection event.
     */
    ERROR,
    /**
     * Represents an unknown or unspecified disconnect reason.
     * This constant is used when the cause of the disconnection cannot be identified
     * or does not fall under any predefined category of disconnect reasons.
     */
    UNKNOWN

}

