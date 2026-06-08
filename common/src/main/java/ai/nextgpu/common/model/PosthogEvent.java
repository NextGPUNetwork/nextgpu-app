package ai.nextgpu.common.model;

public enum PosthogEvent {
    // Performance events
    LLM_QUERY_RESPONSE_TIME,
    SERVER_RESPONSE_TIME_VS_PACKET_SIZE,

    // Reliability events
    CRASH_REPORT,

    // Usability events (counts / actions)
    MESSAGED_PINNED,
    MESSAGED_UNPINNED,
    PROJECT_CREATED,
    CHAT_SESSION_STARTED,
    MODEL_DOWNLOADED,
    IMAGE_GENERATED,
    MESSAGE_SENT,
    APPLICATION_UPTIME,
    THEME_SELECTED,        // with property: dark/light
    MODE_SELECTED,         // with property: advanced/normal
    CHAT_RENAMED,
    CHAT_DELETED,
    APPLICATION_DOWNLOADED,
    CHAT_STARRED,
    CHAT_UNSTARRED,
}
