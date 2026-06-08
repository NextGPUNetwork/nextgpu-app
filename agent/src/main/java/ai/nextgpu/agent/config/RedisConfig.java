package ai.nextgpu.agent.config;

import ai.nextgpu.common.util.JsonUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.embedded.RedisServer;

import java.net.ServerSocket;


@Configuration
public class RedisConfig {
    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private int requestedPort;

    private int embeddedPort;

    @Bean(destroyMethod = "stop")
    public RedisServer embeddedRedisServer() {
        embeddedPort = requestedPort > 0 ? requestedPort : findAvailablePort();
        try {
            RedisServer redisServer = RedisServer.newRedisServer()
                    .port(embeddedPort)
                    .setting("bind 127.0.0.1")
                    .setting("daemonize no")
                    .setting("appendonly no")
                    .setting("save \"\"")
                    .setting("maxmemory 128M")
                    .build();
            redisServer.start();
            return redisServer;
        } catch (Exception e) {
            // Log the error but don't throw to allow context to start in environments
            // where Redis might be provided externally or is not critical for startup.
            System.err.println("Failed to start embedded Redis: " + e.getMessage());
            // Return a dummy instance that isn't started to satisfy dependency injection
            try {
                return RedisServer.newRedisServer()
                        .port(embeddedPort)
                        .build();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    @Bean
    @DependsOn("embeddedRedisServer")
    public RedisConnectionFactory redisConnectionFactory(RedisServer embeddedRedisServer) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(host);
        config.setPort(embeddedPort);
        return new LettuceConnectionFactory(config);
    }

    @Bean(name = "nextGpuAgentRedisTemplate")
    public RedisTemplate<String, Object> agentRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                new GenericJackson2JsonRedisSerializer(JsonUtil.OBJECT_MAPPER);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }

    private int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception e) {
            return 6380; // Fallback
        }
    }
}
