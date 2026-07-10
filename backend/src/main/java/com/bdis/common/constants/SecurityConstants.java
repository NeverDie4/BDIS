package com.bdis.common.constants;

public final class SecurityConstants {

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String BEARER_PREFIX = "Bearer ";

    public static final String ADMIN_ROLE_CODE = "ADMIN";

    public static final String TOKEN_BLACKLIST_PREFIX = "bdis:auth:blacklist:";

    public static final String PERMISSION_CACHE_PREFIX = "bdis:auth:permissions:";

    private SecurityConstants() {}
}
