package cv.igrp.framework.auth.core.route;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/** v3-09: which Redis the permission lookup reads. No Redis server is needed: connections are lazy. */
class PermissionRedisSelectionTest {

    private final RouteSecurityAutoConfiguration configuration = new RouteSecurityAutoConfiguration();

    private PermissionLookup lookup(StaticListableBeanFactory beans) {
        return configuration.igrpPermissionLookup("app", "http://am", Duration.ofSeconds(1),
                beans.getBeanProvider(PermissionRedis.class), beans.getBeanProvider(StringRedisTemplate.class));
    }

    private static Object redisOf(PermissionLookup lookup) {
        return ReflectionTestUtils.getField(lookup, "redis");
    }

    @Test
    @DisplayName("With igrp.security.redis.host, the starter reads through its own connection, not the service's")
    void ownConnection() {
        PermissionRedis own = configuration.igrpPermissionRedis("localhost", 6379, "igrp-reader", "secret",
                Duration.ofSeconds(1));
        try {
            StaticListableBeanFactory beans = new StaticListableBeanFactory();
            beans.addBean("igrpPermissionRedis", own);
            beans.addBean("stringRedisTemplate", new StringRedisTemplate(mock(RedisConnectionFactory.class)));

            assertSame(own.template(), redisOf(lookup(beans)));
        } finally {
            own.destroy();
        }
    }

    @Test
    @DisplayName("Without it, the service's own Redis is used, as before 0.3 (local development)")
    void serviceConnection() {
        StringRedisTemplate service = new StringRedisTemplate(mock(RedisConnectionFactory.class));
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("stringRedisTemplate", service);

        assertSame(service, redisOf(lookup(beans)));
    }

    @Test
    @DisplayName("Without any Redis, there is none: every lookup asks AM")
    void noRedis() {
        assertNull(redisOf(lookup(new StaticListableBeanFactory())));
    }

    @Test
    @DisplayName("A blank username authenticates as the default user; a set one as that user")
    void username() {
        PermissionRedis blank = configuration.igrpPermissionRedis("localhost", 6379, "", "", Duration.ofSeconds(1));
        PermissionRedis reader = configuration.igrpPermissionRedis("localhost", 6379, "igrp-reader", "secret",
                Duration.ofSeconds(1));
        try {
            assertNull(serverOf(blank).getUsername());
            assertEquals("igrp-reader", serverOf(reader).getUsername());
            assertEquals(Duration.ofSeconds(1), ((LettuceConnectionFactory) ReflectionTestUtils.getField(reader, "connections"))
                    .getClientConfiguration().getCommandTimeout());
        } finally {
            blank.destroy();
            reader.destroy();
        }
    }

    private static RedisStandaloneConfiguration serverOf(PermissionRedis redis) {
        return ((LettuceConnectionFactory) ReflectionTestUtils.getField(redis, "connections")).getStandaloneConfiguration();
    }
}
