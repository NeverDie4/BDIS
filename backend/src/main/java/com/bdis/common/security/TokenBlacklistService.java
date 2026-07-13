package com.bdis.common.security;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.UnauthorizedException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(JwtClaims claims) {
        Duration ttl = Duration.between(Instant.now(), claims.expiresAt());
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        try {
            redisTemplate
                    .opsForValue()
                    .set(SecurityConstants.TOKEN_BLACKLIST_PREFIX + claims.jti(), "1", ttl);
        } catch (RedisConnectionFailureException exception) {
            LOGGER.warn("Redis unavailable, failed to blacklist token: {}", exception.getMessage());
            throw new UnauthorizedException("Token blacklist is unavailable");
        }
    }

    public boolean isBlacklisted(String jti) {
        try {
            return Boolean.TRUE.equals(
                    redisTemplate.hasKey(SecurityConstants.TOKEN_BLACKLIST_PREFIX + jti));
        } catch (RedisConnectionFailureException exception) {
            LOGGER.warn(
                    "Redis unavailable, failed to check token blacklist: {}",
                    exception.getMessage());
            throw new UnauthorizedException("Token blacklist is unavailable");
        }
    }
}
