package com.bdis.common.security;

import java.io.Serializable;
import java.time.Instant;

public record SessionAuthenticationDetails(
        String sessionId, String jti, Instant issuedAt, Instant expiresAt)
        implements Serializable {}
