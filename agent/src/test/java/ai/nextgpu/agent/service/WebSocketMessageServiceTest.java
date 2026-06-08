package ai.nextgpu.agent.service;

import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HardwareUtil;
import ai.nextgpu.agent.util.OSUtil;
import ai.nextgpu.common.WebSocketCodes;
import ai.nextgpu.common.dto.WebSocketMessageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.*;

class WebSocketMessageServiceTest {

    @Mock
    private NextGpuAiService aiService;
    @Mock
    private BenchmarkUtil benchmarkUtil;
    @Mock
    private HardwareUtil hardwareUtil;
    @Mock
    private OSUtil osUtil;
    @Mock
    private NextGpuAgentService agentService;
    @Mock
    private AgentSecurityService agentSecurityService;

    private WebSocketMessageService webSocketMessageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        webSocketMessageService = new WebSocketMessageService(aiService, benchmarkUtil, hardwareUtil, osUtil, agentService, agentSecurityService);
    }

    @Test
    void testOnMessageReceivedAcknowledgementFailed() {
        WebSocketMessageDto message = new WebSocketMessageDto();
        message.setMessageCode(WebSocketCodes.ACKNOWLEDGEMENT_FAILED.getValue());
        message.setMessage("Integrity check failed");

        webSocketMessageService.onMessageReceived(message);

        // We can't easily verify log output without extra setup, 
        // but we ensure no exceptions are thrown and it doesn't fall into other cases.
        verifyNoInteractions(aiService);
        verifyNoInteractions(agentService);
    }
}
