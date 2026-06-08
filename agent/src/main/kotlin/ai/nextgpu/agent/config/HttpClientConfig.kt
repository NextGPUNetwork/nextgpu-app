package ai.nextgpu.agent.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
open class HttpClientConfig {

    @Bean
    open fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
}
