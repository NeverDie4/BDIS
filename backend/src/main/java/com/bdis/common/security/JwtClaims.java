package com.bdis.common.security;

import java.time.Instant;

public record JwtClaims(
        Long userId, String username, String jti, Instant issuedAt, Instant expiresAt) {}
