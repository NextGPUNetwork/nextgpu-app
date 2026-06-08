package ai.nextgpu.agent.service;

import ai.nextgpu.agent.repository.GlobalPropertyRepository;
import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.report.HardwareReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class HardwareReportTest {

    @Mock private HardwareUtil hardwareUtil;
    @Mock private BenchmarkUtil benchmarkUtil;
    @Mock private HttpUtil httpUtil;
    @Mock private GlobalPropertyRepository globalPropertyRepository;
    @Mock private DataService dataService;
    @Mock private WebSocketMessageService webSocketMessageService;
    @Mock private ComputerService computerService;
    @Mock private AnalyticsService analyticsService;

    private NextGpuAgentService nextGpuAgentService;

    private final String testWallet = "test-wallet-address";

    @BeforeEach
    void setUp() {
        nextGpuAgentService = new NextGpuAgentService(
                globalPropertyRepository,
                dataService,
                webSocketMessageService,
                hardwareUtil,
                computerService,
                analyticsService
        );
        ReflectionTestUtils.setField(nextGpuAgentService, "loginWallet", testWallet);
    }

    @Test
    void generateComputerHardwareReport_delegatesToComputerService_andReturnsResult() throws Exception {
        HardwareReport expected = new HardwareReport();
        when(computerService.generateComputerHardwareReport(eq(testWallet), anyBoolean())).thenReturn(expected);

        HardwareReport actual = nextGpuAgentService.generateComputerHardwareReport(true);

        assertSame(expected, actual, "Service should return the report from ComputerService");
        verify(computerService, times(1)).generateComputerHardwareReport(testWallet, true);
        verifyNoInteractions(dataService, hardwareUtil, benchmarkUtil, httpUtil);
    }

    @Test
    void generateComputerHardwareReport_whenComputerServiceThrows_propagatesException() throws Exception {
        RuntimeException ex = new RuntimeException("boom");
        when(computerService.generateComputerHardwareReport(eq(testWallet), anyBoolean())).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> nextGpuAgentService.generateComputerHardwareReport(false));
        assertSame(ex, thrown);
        verify(computerService, times(1)).generateComputerHardwareReport(testWallet, false);
    }
}
