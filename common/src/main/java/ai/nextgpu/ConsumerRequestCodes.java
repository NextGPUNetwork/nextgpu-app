package ai.nextgpu.common;

@SuppressWarnings("LombokGetterMayBeUsed")
public enum ConsumerRequestCodes {

    CHAT_QUERY(1),
    SYSTEM_COMMAND(2);

    private final int value;

    ConsumerRequestCodes(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns the ConsumerRequestCodes enum constant associated with the specified integer value.
     *
     * @param value the integer value to look up
     * @return the ConsumerRequestCodes enum constant with the specified value, or null if not found
     */
    public static ConsumerRequestCodes fromValue(int value) {
        for (ConsumerRequestCodes code : ConsumerRequestCodes.values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        return null;
    }
}
