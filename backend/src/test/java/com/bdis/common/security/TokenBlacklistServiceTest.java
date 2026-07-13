package com.bdis.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.UnauthorizedException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class TokenBlacklistServiceTest {

    @Test
    void isBlacklistedFailsClosedWhenRedisUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey("bdis:auth:blacklist:jti-1"))
                .thenThrow(new RedisConnectionFailureException("redis down"));
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);

        assertThatThrownBy(() -> service.isBlacklisted("jti-1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token blacklist is unavailable");
    }

    @Test
    void blacklistFailsClosedWhenRedisUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(operations);
        doThrow(new RedisConnectionFailureException("redis down"))
                .when(operations)
                .set(
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.any());
        TokenBlacklistService service = new TokenBlacklistService(redisTemplate);
        JwtClaims claims =
                new JwtClaims(1L, "admin", "jti-1", Instant.now(), Instant.now().plusSeconds(60));

        assertThatThrownBy(() -> service.blacklist(claims))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Token blacklist is unavailable");

        verify(redisTemplate).opsForValue();
    }
}
