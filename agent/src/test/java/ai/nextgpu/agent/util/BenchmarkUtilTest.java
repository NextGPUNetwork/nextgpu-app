package ai.nextgpu.agent.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import ai.nextgpu.agent.service.BaseTest;
import ai.nextgpu.agent.service.NextGpuAiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class BenchmarkUtilTest extends BaseTest {
    private static final Logger log = LoggerFactory.getLogger(BenchmarkUtilTest.class);
    
    @Autowired
    private NextGpuAiService aiService;

    @Test
    void testBenchmarkMemory() {
        Map<String, Object> scores = benchmarkUtil.benchmarkMemory(true);

        assertNotNull(scores, "Scores should not be null");
        assertTrue(scores.containsKey("write"), "Write score is missing");
        assertTrue(scores.containsKey("read"), "Read score is missing");
        assertTrue(scores.containsKey("latency"), "Latency score is missing");

        int writeSpeed = (int) scores.get("write");
        int readSpeed = (int) scores.get("read");
        int latency = (int) scores.get("latency");

        log.debug("Write speed: {}", writeSpeed);
        log.debug("Read speed: {}", readSpeed);
        log.debug("Latency: {}", latency);

        // Check plausible ranges for the results
        assertTrue(writeSpeed > 0, "Write speed must be positive");
        assertTrue(readSpeed > 0, "Read speed must be positive");
        assertTrue(latency > 0, "Latency must be positive");
    }

    @Test
    void testBenchmarkStorage() {
        Map<String, Object> scores = benchmarkUtil.benchmarkStorage(true);

        assertNotNull(scores, "Scores should not be null");
        if (!scores.containsKey("read_bandwidth_mb") || (int) scores.get("read_bandwidth_mb") == -1) {
            log.debug("Benchmark storage returned no metrics or failed (likely due to missing environment)");
            return;
        }
        assertTrue(scores.containsKey("read_bandwidth_mb"), "Read bandwidth score is missing");
        assertTrue(scores.containsKey("read_iops"), "Read IOPS score is missing");
        assertTrue(scores.containsKey("read_latency_ns"), "Read latency score is missing");
        assertTrue(scores.containsKey("write_bandwidth_mb"), "Write bandwidth score is missing");
        assertTrue(scores.containsKey("write_iops"), "Write IOPS score is missing");
        assertTrue(scores.containsKey("write_latency_ns"), "Write latency score is missing");
        assertTrue(scores.containsKey("total_runtime_ms"), "Total runtime score is missing");

        for (Map.Entry<String, Object> entry : scores.entrySet()) {
            log.debug("{}: {}", entry.getKey(), entry.getValue());
            assertTrue((int) entry.getValue() >= 0, entry.getKey() + " should not have a negative value");
        }
    }

    @Test
    void testBenchmarkCpu() {
        Map<String, Object> cpuResults = benchmarkUtil.benchmarkCpu();

        assertNotNull(cpuResults, "CPU results map should not be null");

        if (cpuResults.isEmpty() || !cpuResults.containsKey("singlecore_events_per_second")) {
            log.debug("Benchmark CPU returned no metrics (likely due to missing environment)");
            return;
        }

        // Validate single-core metrics
        assertTrue(cpuResults.containsKey("singlecore_events_per_second"), "Single-core 'events_per_second' is missing");
        assertTrue(cpuResults.containsKey("singlecore_total_runtime_ms"), "Single-core 'total_runtime_ms' is missing");

        // Validate multi-core metrics
        assertTrue(cpuResults.containsKey("multicore_events_per_second"), "Multi-core 'events_per_second' is missing");
        assertTrue(cpuResults.containsKey("multicore_total_runtime_ms"), "Multi-core 'total_runtime_ms' is missing");

        int singleCpuEvents = (int) cpuResults.getOrDefault("singlecore_events_per_second", 0);
        int multiCpuEvents = (int) cpuResults.getOrDefault("multicore_events_per_second", 0);
        assertTrue(multiCpuEvents >= singleCpuEvents, "Multi CPU score should be higher than or equal to single CPU score");
    }

    @Test
    void testBenchmarkGpu() {
        // Mocking AI service response
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseNode = mapper.createObjectNode();
        responseNode.put("eval_count", 50);
        responseNode.put("eval_duration", 1000000000L); // 1 second in nanoseconds

        when(aiService.generateResponseRaw(anyString(), anyString(), anyList(), anyFloat()))
                .thenReturn(responseNode);

        Map<String, Object> gpuResults = benchmarkUtil.benchmarkGpu();

        assertNotNull(gpuResults, "GPU results should not be null");
        assertTrue(gpuResults.containsKey("gpu0"), "GPU0 results missing");
        
        Map<String, Object> scores = (Map<String, Object>) gpuResults.get("gpu0");
        assertEquals(50, scores.get("tokens"));
        assertEquals(1000000000L, ((Number) scores.get("elapsedTime")).longValue());
        assertEquals(50.0, (double) scores.get("tokens_per_second"), 0.1);
    }

    @Test
    void testBenchmarkGpuFailure() {
        // Mock AI service failure
        when(aiService.generateResponseRaw(anyString(), anyString(), anyList(), anyFloat()))
                .thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> benchmarkUtil.benchmarkGpu());

        assertEquals("Failed to run GPU benchmark", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Invalid response from AI service for GPU benchmark",
                exception.getCause().getMessage());
    }

    @Test
    void testBenchmarkStorageFailure() {
        // Mock invalid distro to trigger failure
        when(service.getGlobalProperty(anyString())).thenReturn(null);

        Map<String, Object> scores = benchmarkUtil.benchmarkStorage(true);

        assertNotNull(scores, "Scores should not be null");
        assertEquals(-1, scores.get("read_bandwidth_mb"));
        assertEquals(-1, scores.get("total_runtime_ms"));
    }
}
