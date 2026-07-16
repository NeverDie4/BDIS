package com.bdis.common.security;

import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.core.Result;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.PasswordChangeRequiredException;
import com.bdis.common.exception.UnauthorizedException;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    private final TokenBlacklistService tokenBlacklistService;

    private final CurrentUserService currentUserService;

    private final UserSessionService userSessionService;

    private final UserMapper userMapper;

    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(
            JwtUtils jwtUtils,
            TokenBlacklistService tokenBlacklistService,
            CurrentUserService currentUserService,
            UserSessionService userSessionService,
            UserMapper userMapper,
            ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.tokenBlacklistService = tokenBlacklistService;
        this.currentUserService = currentUserService;
        this.userSessionService = userSessionService;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (header == null || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            String token = header.substring(SecurityConstants.BEARER_PREFIX.length());
            JwtClaims claims = jwtUtils.parse(token);
            if (tokenBlacklistService.isBlacklisted(claims.jti())) {
                throw new UnauthorizedException("Token 已退出登录");
            }
            UserSessionEntity session = userSessionService.validate(claims);
            CurrentUser user = currentUserService.load(claims.userId());
            requirePasswordChangedIfNecessary(user, request);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getPermissions().stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList());
            authentication.setDetails(
                    new SessionAuthenticationDetails(
                            session.getSessionId(),
                            claims.jti(),
                            claims.issuedAt(),
                            claims.expiresAt()));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (UnauthorizedException exception) {
            SecurityContextHolder.clearContext();
            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ResultCodeEnum.UNAUTHORIZED,
                    exception.getMessage());
        } catch (PasswordChangeRequiredException exception) {
            SecurityContextHolder.clearContext();
            writeError(
                    response,
                    HttpServletResponse.SC_FORBIDDEN,
                    ResultCodeEnum.PASSWORD_CHANGE_REQUIRED,
                    exception.getMessage());
        }
    }

    private void requirePasswordChangedIfNecessary(CurrentUser user, HttpServletRequest request) {
        UserEntity entity = userMapper.selectById(user.getUserId());
        if (entity == null || !Boolean.TRUE.equals(entity.getMustChangePassword())) {
            return;
        }
        String uri = request.getRequestURI();
        String method = request.getMethod();
        boolean allowed =
                ("GET".equalsIgnoreCase(method) && uri.endsWith("/auth/me"))
                        || ("DELETE".equalsIgnoreCase(method)
                                && uri.endsWith("/auth/sessions/current"))
                        || ("PUT".equalsIgnoreCase(method) && uri.endsWith("/me/password"))
                        || ("GET".equalsIgnoreCase(method) && uri.endsWith("/me/sessions"));
        if (!allowed) {
            throw new PasswordChangeRequiredException();
        }
    }

    private void writeError(
            HttpServletResponse response, int status, ResultCodeEnum code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter()
                .write(objectMapper.writeValueAsString(Result.error(code, message, null)));
    }
}
