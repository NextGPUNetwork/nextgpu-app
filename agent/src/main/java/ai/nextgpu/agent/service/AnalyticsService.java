package ai.nextgpu.agent.service;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.CpuRepository;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.model.PosthogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private static final String ANALYTICS_EVENTS_KEY = "analytics:events";

    private final CpuRepository cpuRepository;

    private final RedisTemplate<String, Object> redisTemplate;

    private final NextGpuWebService nextGpuWebService;

    private final HardwareUtil hardwareUtil;

    private final String machineHash;

    @Autowired
    public AnalyticsService(
            CpuRepository cpuRepository,
            @Qualifier("nextGpuAgentRedisTemplate") RedisTemplate<String, Object> redisTemplate,
            NextGpuWebService nextGpuWebService, HardwareUtil hardwareUtil
    ) {
        this.cpuRepository = cpuRepository;
        this.redisTemplate = redisTemplate;
        this.nextGpuWebService = nextGpuWebService;
        this.hardwareUtil = hardwareUtil;
        this.machineHash = HardwareUtil.generateHardwareFingerprint();
    }

    /**
     * Periodically flushes stored analytics events to the NextGPU web API.
     * The default interval is 1 minute, configurable via application.yml.
     */
    @Scheduled(fixedDelayString = "${nextgpu.agent.analytics.http-request-period-ms:60000}")
    @SuppressWarnings("unchecked")
    public void sendAnalyticsEventsPeriodically() {
        try {
            Map<Object, Object> storedEvents = getAllEvents();
            if (storedEvents.isEmpty()) {
                return;
            }

            /* ** storedEvents example ** */
            /*
                {
                   "8f2c2d2b-6d7a-4c7d-9d7a-11f7c3b2d101": {
                     "APPLICATION_UPTIME": {
                       "app_uptime_duration": 15420
                     }
                   },
                   "b91a1d0f-3d8e-4f50-9c2b-8f2a8e9d2207": {
                     "CRASH_REPORT": {
                       "exception_class": "java.lang.RuntimeException",
                       "message": "Database connection failed",
                       "stacktrace": "java.lang.RuntimeException: Database connection failed\n..."
                     }
                   },
                   "f3a6f2d4-1b99-4b2b-a9ef-2c1f73ef4c88": {
                     "MESSAGE_SENT": {
                       "message_length": 128,
                       "model": "gpt-4.1-mini"
                     }
                   }
                 }
             */
            /* *********************************/


            // Map(Event Name -> Event Data)
            Map<String, Object> events = storedEvents.values().stream()
                    .filter(Map.class::isInstance)
                    .map(event -> (Map<String, Object>) event)
                    .collect(Collectors.toMap(
                        event -> event.keySet().iterator().next(),
                        event -> event.values().iterator().next(),
                        (existing, replacement) -> existing
                    ));

            if (events.isEmpty() || machineHash == null) {
                return;
            }

            clearEvents();
            nextGpuWebService.postEventDataInBatch(machineHash, events);

            log.debug("Sent {} analytics events to NextGPU API.", events.size());
        } catch (Exception e) {
            log.error("Failed to send analytics events periodically.", e);
        }
    }

    /**
     * Captures and logs an event with the specified name and associated data.
     * This method sends the event data along with a unique machine identifier
     * to a PostHog analytics service for processing.
     *
     * @param eventName the name of the event to be captured
     * @param eventData a map containing key-value pairs representing
     *                  additional event data or properties
     */
    public void captureEvent(String eventName, Map<String, Object> eventData) {
        log.debug("Capturing event: {} with properties: {}", eventName, eventData);
        Map<String, Object> event = Map.of(eventName, eventData);
        storeEvent(event);
    }


    /**
     * Sends an analytics event capturing the application's uptime duration.
     * <br>
     * This method retrieves the timestamp of the application's startup from the global property repository.
     * If the timestamp exists, it calculates the current uptime by subtracting the startup timestamp
     * from the current system time in milliseconds. The calculated uptime duration is then sent to the
     * analytics service as part of an event payload.
     * <br>
     * <strong>Preconditions:</strong>
     * <br>- The global property with the name matching the application's startup timestamp is expected to exist.
     * <br>- The value stored in this global property must be a valid long value representing a Unix timestamp in milliseconds.
     * <br>
     * <strong>Operation Details:</strong>
     * <br>- Fetches the global property corresponding to the application startup timestamp.
     * <br>- If the global property exists and its value is valid, computes the uptime duration.
     * <br>- Sends the computed uptime duration as part of an analytics event with the name `PosthogEvent.APPLICATION_UPTIME`.
     * <br>
     * <br>
     * No action is performed if the global property is missing or its value is invalid.
     */
    public void notifyApplicationUpTime() {

        if (GlobalPropertyConfig.APPLICATION_UP_TIMESTAMP == null)
            return;
        long uptime = System.currentTimeMillis() - GlobalPropertyConfig.APPLICATION_UP_TIMESTAMP;
        this.captureEvent(PosthogEvent.APPLICATION_UPTIME.name(), Map.of("app_uptime_duration", uptime));

        // Send Current Events before closing the application
        sendAnalyticsEventsPeriodically();
    }

    /**
     * Captures and notifies a crash report to the analytics service.
     * This method extracts the exception class name and stack trace from the provided
     * {@link Throwable} and sends it as a CRASH_REPORT event.
     *
     * @param throwable the exception or error that caused the crash
     */
    public void notifyCrashReport(Throwable throwable) {
        if (throwable == null) return;

        log.error("Capturing crash report for: {}", throwable.getMessage(), throwable);

        String stackTrace = getStackTrace(throwable);
        Map<String, Object> eventData = Map.of(
                "exception_class", throwable.getClass().getName(),
                "message", throwable.getMessage() != null ? throwable.getMessage() : "No message",
                "stacktrace", stackTrace
        );

        this.captureEvent(PosthogEvent.CRASH_REPORT.name(), eventData);
    }

    /**
     * Utility method to convert a {@link Throwable} stack trace into a string.
     *
     * @param throwable the throwable to extract the stack trace from
     * @return the string representation of the stack trace
     */
    private String getStackTrace(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }


    /**
     * Stores an event in the Redis datastore.
     * The event is stored under a unique identifier generated using UUID.
     *
     * @param event the event to be stored; it can be any object representing event data
     */
    public void storeEvent(Object event) {
        String eventId = UUID.randomUUID().toString();
        HashOperations<String, Object, Object> hashOps = redisTemplate.opsForHash();
        hashOps.put(ANALYTICS_EVENTS_KEY, eventId, event);
    }

    /**
     * Retrieves all analytics events stored in the Redis hash with the key "analytics:events".
     *
     * @return a map containing all stored events, where each key represents a unique event identifier
     *         and the corresponding value is the event data.
     */
    public Map<Object, Object> getAllEvents() {
        return redisTemplate.opsForHash().entries(ANALYTICS_EVENTS_KEY);
    }

    /**
     * Removes all stored analytics events from the Redis data store.
     *
     * This method deletes the key "analytics:events" from the Redis database,
     * effectively clearing all events that were previously stored.
     */
    public void clearEvents() {
        redisTemplate.delete(ANALYTICS_EVENTS_KEY);
    }
}
