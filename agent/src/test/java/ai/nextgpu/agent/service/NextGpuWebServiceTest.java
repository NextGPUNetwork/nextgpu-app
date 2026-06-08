package ai.nextgpu.agent.service;

import ai.nextgpu.agent.util.HttpUtil;
import ai.nextgpu.common.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NextGpuWebServiceTest {

    @Mock
    private HttpUtil httpUtil;

    private NextGpuWebService service;

    @BeforeEach
    void setUp() {
        service = new NextGpuWebService(httpUtil);
        ReflectionTestUtils.setField(service, "BASE_URL", "http://localhost:8080");
    }

    @Test
    void getAvailableModelsByGpu_callsExpectedEndpoint_andParsesResponse() throws Exception {
        String jsonResponse = """
                    [
                      { "model": "deepseek-r1:14b", "type": "general", "sizeInGB": 14 },
                      { "model": "deepseek-coder:6.7b", "type": "programming", "sizeInGB": 7 }
                    ]
                """;
        
        when(httpUtil.post(anyString(), any(), eq(String.class), eq(true))).thenReturn(jsonResponse);

        GpuDto gpuDto = new GpuDto();
        List<AiModelDto> result = service.getAvailableModelsByGpu(gpuDto);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("deepseek-r1:14b", result.get(0).getModel());
        assertEquals("deepseek-coder:6.7b", result.get(1).getModel());

        verify(httpUtil).post(endsWith("/api/ai/models"), eq(gpuDto), eq(String.class), eq(true));
    }

    @Test
    void getUserProfile_callsGetWithAuth() throws Exception {
        UserProfileDto profile = new UserProfileDto();
        profile.setWalletAddress("0x123");
        
        when(httpUtil.get(anyString(), eq(UserProfileDto.class), eq(true))).thenReturn(profile);
        
        UserProfileDto result = service.getUserProfile("0x123");
        
        assertEquals("0x123", result.getWalletAddress());
        verify(httpUtil).get(contains("/users/0x123"), eq(UserProfileDto.class), eq(true));
    }

    @Test
    void saveBenchmarkReport_callsPostWithAuth() throws Exception {
        BenchmarkReportDto reportDto = new BenchmarkReportDto();
        
        service.saveBenchmarkReport(reportDto);
        
        verify(httpUtil).post(endsWith("/reports/benchmark"), eq(reportDto), eq(BenchmarkReportDto.class), eq(true));
    }
}
