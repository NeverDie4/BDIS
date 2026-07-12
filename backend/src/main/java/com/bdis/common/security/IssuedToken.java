package com.bdis.common.security;

import java.time.Instant;

public record IssuedToken(String accessToken, String jti, Instant issuedAt, Instant expiresAt) {}
