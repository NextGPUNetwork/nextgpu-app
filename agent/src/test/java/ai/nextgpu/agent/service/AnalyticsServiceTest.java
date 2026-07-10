package ai.nextgpu.agent.service;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.agent.repository.CpuRepository;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.common.model.Cpu;
import ai.nextgpu.common.model.PosthogEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private CpuRepository cpuRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOperations;
    @Mock
    private NextGpuWebService nextGpuWebService;
    private MockedStatic<HardwareUtil> hardwareUtilMock;
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        hardwareUtilMock = mockStatic(HardwareUtil.class);

        hardwareUtilMock.when(HardwareUtil::generateHardwareFingerprint)
                .thenReturn("machine-hash");

        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        analyticsService = new AnalyticsService(
                cpuRepository,
                redisTemplate,
                nextGpuWebService,
                mock(HardwareUtil.class)
        );
    }

    @AfterEach
    void tearDown() {
        hardwareUtilMock.close();
    }

    @Test
    void testSendAnalyticsEventsPeriodically_WithEvents() throws Exception {
        Map<Object, Object> storedEvents = Map.of(
                "id1", Map.of("event1", Map.of("prop1", "val1")),
                "id2", Map.of("event2", Map.of("prop2", "val2"))
        );
        when(hashOperations.entries("analytics:events")).thenReturn(storedEvents);

        analyticsService.sendAnalyticsEventsPeriodically();

        verify(nextGpuWebService).postEventDataInBatch(eq("machine-hash"), anyMap());
        verify(redisTemplate).delete("analytics:events");
    }

    @Test
    void testNotifyApplicationUpTime() {
        GlobalPropertyConfig.APPLICATION_UP_TIMESTAMP = System.currentTimeMillis() - 5000;
        when(hashOperations.entries("analytics:events"))
                .thenReturn(Map.of(
                        "id1",
                        Map.of(
                                PosthogEvent.APPLICATION_UPTIME.name(),
                                Map.of("app_uptime_duration", 5000L)
                        )
                ));

        analyticsService.notifyApplicationUpTime();



        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations).put(eq("analytics:events"), anyString(), eventCaptor.capture());

        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        assertTrue(event.containsKey(PosthogEvent.APPLICATION_UPTIME.name()));
        Map<String, Object> data = (Map<String, Object>) event.get(PosthogEvent.APPLICATION_UPTIME.name());
        assertTrue((Long) data.get("app_uptime_duration") >= 5000);
    }

    @Test
    void testNotifyCrashReport_WithThrowableMessage() {
        Throwable t = new Throwable("Custom message");
        analyticsService.notifyCrashReport(t);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations).put(eq("analytics:events"), anyString(), eventCaptor.capture());

        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        Map<String, Object> data = (Map<String, Object>) event.get(PosthogEvent.CRASH_REPORT.name());
        assertEquals("Custom message", data.get("message"));
        assertEquals("java.lang.Throwable", data.get("exception_class"));
        assertNotNull(data.get("stacktrace"));
    }

    @Test
    void testNotifyCrashReport_WithoutThrowableMessage() {
        Throwable t = new Throwable();
        analyticsService.notifyCrashReport(t);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations).put(eq("analytics:events"), anyString(), eventCaptor.capture());

        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        Map<String, Object> data = (Map<String, Object>) event.get(PosthogEvent.CRASH_REPORT.name());
        assertEquals("No message", data.get("message"));
    }

    @Test
    void testNotifyCrashReport_Null() {
        analyticsService.notifyCrashReport(null);
        verifyNoInteractions(hashOperations);
    }
}
