package com.bdis.common.security;

import com.bdis.common.constants.SecurityConstants;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(JwtClaims claims) {
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        redisTemplate
                .opsForValue()
                .set(SecurityConstants.TOKEN_BLACKLIST_PREFIX + claims.jti(), "1", ttl);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(SecurityConstants.TOKEN_BLACKLIST_PREFIX + jti));
    }
}
