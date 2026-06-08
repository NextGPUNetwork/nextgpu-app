package ai.nextgpu.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {

    private static final String THREAD_NAME_PREFIX = "NextGPU-Agent-Scheduler-";
    @Value("${nextgpu.agent.threadPoolSize:5}")
    private int THREAD_POOL_SIZE;

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(THREAD_POOL_SIZE);
        scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
        return scheduler;
    }
}
