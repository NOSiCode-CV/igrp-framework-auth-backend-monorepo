package cv.igrp.framework.auth.core.route;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * The starter's own connection to the shared Redis where AM writes the permission sets
 * (v3-09). It is separate from the service's own Redis ({@code spring.data.redis.*}), so the
 * read-only user that the Redis ACL gives services never applies to the service's own data.
 *
 * <p>Not a {@code RedisConnectionFactory} bean: one would replace the service's own
 * auto-configured connection.
 */
public final class PermissionRedis implements DisposableBean {

    /** Where the shared Redis is and who the service is there ({@code igrp.security.redis.*}). */
    public record Settings(String host, int port, String username, String password, Duration timeout) {
    }

    private final LettuceConnectionFactory connections;
    private final StringRedisTemplate template;

    public PermissionRedis(Settings settings) {
        RedisStandaloneConfiguration server = new RedisStandaloneConfiguration(settings.host(), settings.port());
        if (StringUtils.hasText(settings.username())) {
            server.setUsername(settings.username());
        }
        server.setPassword(RedisPassword.of(settings.password()));
        connections = new LettuceConnectionFactory(server,
                LettuceClientConfiguration.builder().commandTimeout(settings.timeout()).build());
        connections.afterPropertiesSet();
        connections.start();
        template = new StringRedisTemplate(connections);
    }

    /** Reads the permission sets. */
    public StringRedisTemplate template() {
        return template;
    }

    @Override
    public void destroy() {
        connections.destroy();
    }
}
