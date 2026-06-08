package ai.nextgpu.agent.service;

import ai.nextgpu.agent.config.GlobalPropertyConfig;
import ai.nextgpu.common.model.GlobalProperty;
import ai.nextgpu.agent.repository.*;
import ai.nextgpu.agent.util.BenchmarkUtil;
import ai.nextgpu.agent.util.HttpUtil;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = BaseTest.TestConfig.class)
public class BaseTest {

    /**
     * This test class validates the behavior of the `isLoggedIn` method of the `NextGpuAgentService` class.
     * The method determines whether the user is logged in based on the presence and state of a JWT token
     * stored in the `GlobalPropertyRepository`.
     */
    @Configuration
    @SpringBootApplication(scanBasePackages = {"ai.nextgpu.agent.util", "ai.nextgpu.agent.service"})
    public static class TestConfig {
        @Bean
        public GlobalPropertyRepository globalPropertyRepository() {
            return mock(GlobalPropertyRepository.class);
        }

        @Bean
        public HttpUtil httpUtil() {
            return mock(HttpUtil.class);
        }

        @Bean
        public BenchmarkUtil benchmarkUtil() {
            return new BenchmarkUtil();
        }

        @Bean
        public DataService dataService() {
            return mock(DataService.class);
        }

        @Bean
        public NextGpuWebService nextGpuWebService() {
            return mock(NextGpuWebService.class);
        }

        @Bean
        public NextGpuAgentService nextGpuAgentService() {
            return mock(NextGpuAgentService.class);
        }

        @Bean
        public NextGpuAiService nextGpuAiService() {
            return mock(NextGpuAiService.class);
        }

        @Bean
        public AnalyticsService analyticsService() {
            return mock(AnalyticsService.class);
        }

        @Bean
        public GlobalPropertyConfig globalPropertyConfig(GlobalPropertyRepository globalPropertyRepository) {
            return new GlobalPropertyConfig(globalPropertyRepository);
        }

        @Bean
        public ChatMessageRepository chatMessageRepository() {
            return mock(ChatMessageRepository.class);
        }

        @Bean
        public ChatSessionRepository chatSessionRepository() {
            return mock(ChatSessionRepository.class);
        }

        @Bean
        public ProjectRepository projectRepository() {
            return mock(ProjectRepository.class);
        }
    }

    @Autowired
    protected NextGpuAgentService service;

    @Autowired
    protected GlobalPropertyRepository globalPropertyRepository;

    @Autowired
    protected HttpUtil httpUtil;

    @Autowired
    protected BenchmarkUtil benchmarkUtil;

        @BeforeEach
        void setUp() {
            // Mock the global properties needed for benchmarkStorage
            GlobalProperty usernameProperty = new GlobalProperty();
            usernameProperty.setValueReference("nextgpu");

            GlobalProperty distroProperty = new GlobalProperty();
            distroProperty.setValueReference("nextgpu");

            GlobalProperty tokenLimitProperty = new GlobalProperty();
            tokenLimitProperty.setValueReference("128000");

            when(globalPropertyRepository.findByName(GlobalPropertyConfig.OS_USERNAME)).thenReturn(Optional.of(usernameProperty));
            when(globalPropertyRepository.findByName(GlobalPropertyConfig.LINUX_DISTRO)).thenReturn(Optional.of(distroProperty));
            when(globalPropertyRepository.findByName(GlobalPropertyConfig.TOKEN_HISTORY_LIMIT)).thenReturn(Optional.of(tokenLimitProperty));
            when(globalPropertyRepository.findByName(GlobalPropertyConfig.MAX_PINNED_MESSAGES)).thenReturn(Optional.of(tokenLimitProperty));

            when(service.getGlobalProperty(GlobalPropertyConfig.OS_USERNAME)).thenReturn(usernameProperty);
            when(service.getGlobalProperty(GlobalPropertyConfig.LINUX_DISTRO)).thenReturn(distroProperty);
        }
}
