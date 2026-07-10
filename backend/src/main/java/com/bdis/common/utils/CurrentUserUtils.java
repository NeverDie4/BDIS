package com.bdis.common.utils;

import com.bdis.common.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class CurrentUserUtils {

    private CurrentUserUtils() {}

    public static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return 0L;
        }
        if (authentication.getPrincipal() instanceof CurrentUser user) {
            return user.getUserId();
        }
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    public static String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return "anonymous";
        }
        if (authentication.getPrincipal() instanceof CurrentUser user) {
            return user.getUsername();
        }
        return authentication.getName();
    }

    public static Set<String> currentRoleCodes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CurrentUser user) {
            return user.getRoleCodes();
        }
        return Set.of();
    }

    public static String currentIp() {
        return currentRequest()
                .map(
                        request ->
                                Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                                        .filter(value -> !value.isBlank())
                                        .map(value -> value.split(",")[0].trim())
                                        .orElse(request.getRemoteAddr()))
                .orElse(null);
    }

    public static String currentUserAgent() {
        return currentRequest().map(request -> request.getHeader("User-Agent")).orElse(null);
    }

    public static String currentRequestMethod() {
        return currentRequest().map(HttpServletRequest::getMethod).orElse(null);
    }

    public static String currentRequestUri() {
        return currentRequest().map(HttpServletRequest::getRequestURI).orElse(null);
    }

    private static Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }
}
