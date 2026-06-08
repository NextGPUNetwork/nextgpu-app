package ai.nextgpu.common;

@SuppressWarnings("LombokGetterMayBeUsed")
public enum WebSocketCodes {

    HEARTBEAT(1),   // Represents that the client is checking if the server is available

    READY_TO_SERVE(2),   // Represents that the Provider is available to serve the server
    UNABLE_TO_SERVE(3), // Represents that the Provider is not available to serve the server
    HARDWARE_REPORT(4), // Represents that either Provider is sending hardware report, or Consumer is requesting it
    BENCHMARK_REPORT(5),    // Represents that the Provider is sending benchmark report, or Consumer is requesting it
    USAGE_REPORT(6),    // Represents that the Provider is sending computer's usage report or Consumer is requesting it
    CONNECT_WITH_PROVIDER(7),    // Represents that the Consumer is connecting with a provider
    DISCONNECT_FROM_PROVIDER(8),    // Represents that the Consumer is disconnecting from a provider
    PROVIDER_REQUEST(9),    // Provider is making a generic request to server
    PROVIDER_RESPONSE(10),    // Provider is responding to a previous request
    CONSUMER_REQUEST(11),   // Consumer is making a generic request to the Provider
    CONNECT_WITH_CONSUMER(12), // Consumer is connecting with a Provider
    CONNECTION_FAILED(13), // Represents that the connection attempt failed
    ACKNOWLEDGEMENT_FAILED(14), // Represents that the acknowledgement failed
    ;

    // CONNECTION_FAILED: The system uses this code to send a message to the user if the connection attempt fails
    // CONNECT_WITH_CONSUMER: The system uses this code to send the consumer public key to the Provider Personal Computer

    private final int value;

    WebSocketCodes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns the WebSocketCodes enum constant associated with the specified integer value.
     *
     * @param value the integer value to look up
     * @return the WebSocketCodes enum constant with the specified value, or null if not found
     */
    public static WebSocketCodes fromValue(int value) {
        for (WebSocketCodes code : WebSocketCodes.values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        return null;
    }

    /**
     * Returns true for codes that are infrastructure-level and
     * should never be cached or tracked.
     */
    public static boolean isTransient(Integer code) {
        if (code == null) return false;
        return fromValue(code) == HEARTBEAT;
    }
}
