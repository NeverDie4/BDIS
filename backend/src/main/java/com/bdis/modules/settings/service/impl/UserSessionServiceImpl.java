package com.bdis.modules.settings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.exception.UnauthorizedException;
import com.bdis.common.security.IssuedToken;
import com.bdis.common.security.JwtClaims;
import com.bdis.common.security.SecurityUtils;
import com.bdis.common.security.SessionAuthenticationDetails;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.mapper.UserSessionMapper;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.settings.vo.UserSessionVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserSessionServiceImpl implements UserSessionService {

    private static final String ACTIVE = "active";

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    private final UserSessionMapper sessionMapper;

    private final UserMapper userMapper;

    private final TokenBlacklistService tokenBlacklistService;

    public UserSessionServiceImpl(
            UserSessionMapper sessionMapper,
            UserMapper userMapper,
            TokenBlacklistService tokenBlacklistService) {
        this.sessionMapper = sessionMapper;
        this.userMapper = userMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    @Transactional
    public SessionAuthenticationDetails create(
            Long userId, IssuedToken token, HttpServletRequest request) {
        UserSessionEntity session = new UserSessionEntity();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTokenJti(token.jti());
        session.setClientType(resolveClientType(request.getHeader("User-Agent")));
        session.setDeviceName(resolveDeviceName(request.getHeader("User-Agent")));
        session.setIpAddress(clientIp(request));
        session.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
        session.setIssuedAt(toLocalDateTime(token.issuedAt()));
        session.setLastActiveAt(toLocalDateTime(token.issuedAt()));
        session.setExpiresAt(toLocalDateTime(token.expiresAt()));
        session.setSessionStatus(ACTIVE);
        sessionMapper.insert(session);
        return new SessionAuthenticationDetails(
                session.getSessionId(), token.jti(), token.issuedAt(), token.expiresAt());
    }

    @Override
    @Transactional
    public UserSessionEntity validate(JwtClaims claims) {
        UserSessionEntity session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getTokenJti, claims.jti())
                                .last("limit 1"));
        if (session == null || !session.getUserId().equals(claims.userId())) {
            throw new UnauthorizedException("登录会话不存在，请重新登录");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!ACTIVE.equals(session.getSessionStatus()) || !now.isBefore(session.getExpiresAt())) {
            if (ACTIVE.equals(session.getSessionStatus())) {
                session.setSessionStatus("expired");
                sessionMapper.updateById(session);
            }
            throw new UnauthorizedException("登录会话已失效，请重新登录");
        }
        UserEntity user = userMapper.selectById(claims.userId());
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UnauthorizedException("账号不存在或已停用");
        }
        if (user.getPasswordChangedAt() != null
                && claims.issuedAt()
                        .isBefore(user.getPasswordChangedAt().atZone(SYSTEM_ZONE).toInstant())) {
            throw new UnauthorizedException("密码已变更，请重新登录");
        }
        if (session.getLastActiveAt().plusMinutes(5).isBefore(now)) {
            session.setLastActiveAt(now);
            sessionMapper.updateById(session);
        }
        return session;
    }

    @Override
    @Transactional
    public void revokeByJti(String jti, String reason) {
        UserSessionEntity session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getTokenJti, jti)
                                .last("limit 1"));
        if (session == null || !ACTIVE.equals(session.getSessionStatus())) {
            return;
        }
        session.setSessionStatus("revoked");
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokeReason(reason);
        sessionMapper.updateById(session);
    }

    @Override
    public List<UserSessionVO> listCurrentUserSessions() {
        Long userId = SecurityUtils.currentUser().getUserId();
        String currentJti = SecurityUtils.currentSession().jti();
        return sessionMapper
                .selectList(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getUserId, userId)
                                .orderByDesc(UserSessionEntity::getLastActiveAt))
                .stream()
                .map(session -> toVO(session, currentJti))
                .toList();
    }

    @Override
    @Transactional
    public void revokeCurrentUserSession(String sessionId) {
        Long userId = SecurityUtils.currentUser().getUserId();
        UserSessionEntity session =
                sessionMapper.selectOne(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getSessionId, sessionId)
                                .eq(UserSessionEntity::getUserId, userId)
                                .last("limit 1"));
        if (session == null) {
            throw new ResourceNotFoundException("会话不存在");
        }
        revoke(session, "user_revoked");
    }

    @Override
    @Transactional
    public void revokeOtherCurrentUserSessions() {
        Long userId = SecurityUtils.currentUser().getUserId();
        String currentJti = SecurityUtils.currentSession().jti();
        List<UserSessionEntity> sessions =
                sessionMapper.selectList(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getUserId, userId)
                                .eq(UserSessionEntity::getSessionStatus, ACTIVE)
                                .ne(UserSessionEntity::getTokenJti, currentJti));
        sessions.forEach(session -> revoke(session, "user_revoked_others"));
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId, String reason) {
        List<UserSessionEntity> sessions =
                sessionMapper.selectList(
                        new LambdaQueryWrapper<UserSessionEntity>()
                                .eq(UserSessionEntity::getUserId, userId)
                                .eq(UserSessionEntity::getSessionStatus, ACTIVE));
        sessions.forEach(session -> revoke(session, reason));
    }

    private void revoke(UserSessionEntity session, String reason) {
        if (!ACTIVE.equals(session.getSessionStatus())) {
            return;
        }
        session.setSessionStatus("revoked");
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokeReason(reason);
        sessionMapper.updateById(session);
        tokenBlacklistService.blacklist(
                new JwtClaims(
                        session.getUserId(),
                        "",
                        session.getTokenJti(),
                        session.getIssuedAt().atZone(SYSTEM_ZONE).toInstant(),
                        session.getExpiresAt().atZone(SYSTEM_ZONE).toInstant()));
    }

    private UserSessionVO toVO(UserSessionEntity session, String currentJti) {
        UserSessionVO vo = new UserSessionVO();
        vo.setSessionId(session.getSessionId());
        vo.setCurrent(session.getTokenJti().equals(currentJti));
        vo.setClientType(session.getClientType());
        vo.setDeviceName(session.getDeviceName());
        vo.setIpAddress(session.getIpAddress());
        vo.setIssuedAt(session.getIssuedAt());
        vo.setLastActiveAt(session.getLastActiveAt());
        vo.setExpiresAt(session.getExpiresAt());
        vo.setStatus(session.getSessionStatus());
        return vo;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, SYSTEM_ZONE);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return truncate(forwardedFor.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    private String resolveClientType(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "unknown";
        }
        String value = userAgent.toLowerCase(Locale.ROOT);
        return value.contains("mobile") || value.contains("android") ? "mobile" : "web";
    }

    private String resolveDeviceName(String userAgent) {
        if (!StringUtils.hasText(userAgent)) {
            return "未知设备";
        }
        String value = userAgent.toLowerCase(Locale.ROOT);
        String browser =
                value.contains("edg/")
                        ? "Edge"
                        : value.contains("chrome/")
                                ? "Chromium"
                                : value.contains("firefox/") ? "Firefox" : "浏览器";
        String system =
                value.contains("windows")
                        ? "Windows"
                        : value.contains("android")
                                ? "Android"
                                : value.contains("iphone") || value.contains("ipad")
                                        ? "iOS"
                                        : value.contains("linux") ? "Linux" : "未知系统";
        return browser + " on " + system;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
