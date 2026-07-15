package com.bdis.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.service.LoginLogService;
import com.bdis.common.constants.SecurityConstants;
import com.bdis.common.exception.DuplicateResourceException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.UnauthorizedException;
import com.bdis.common.security.BootstrapProperties;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.IssuedToken;
import com.bdis.common.security.JwtClaims;
import com.bdis.common.security.JwtProperties;
import com.bdis.common.security.JwtUtils;
import com.bdis.common.security.SecurityUtils;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.modules.auth.dto.BootstrapAdminDTO;
import com.bdis.modules.auth.dto.LoginDTO;
import com.bdis.modules.auth.dto.RefreshSessionDTO;
import com.bdis.modules.auth.service.AuthService;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.auth.vo.CurrentUserVO;
import com.bdis.modules.auth.vo.LoginVO;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.service.UserPreferenceService;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.entity.RoleEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.entity.UserRoleEntity;
import com.bdis.modules.user.mapper.RoleMapper;
import com.bdis.modules.user.mapper.UserMapper;
import com.bdis.modules.user.mapper.UserRoleMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final LoginLogService loginLogService;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtils jwtUtils;

    private final JwtProperties jwtProperties;

    private final BootstrapProperties bootstrapProperties;

    private final TokenBlacklistService tokenBlacklistService;

    private final CurrentUserService currentUserService;

    private final UserSessionService userSessionService;

    private final UserPreferenceService userPreferenceService;

    public AuthServiceImpl(
            UserMapper userMapper,
            RoleMapper roleMapper,
            UserRoleMapper userRoleMapper,
            LoginLogService loginLogService,
            PasswordEncoder passwordEncoder,
            JwtUtils jwtUtils,
            JwtProperties jwtProperties,
            BootstrapProperties bootstrapProperties,
            TokenBlacklistService tokenBlacklistService,
            CurrentUserService currentUserService,
            UserSessionService userSessionService,
            UserPreferenceService userPreferenceService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.loginLogService = loginLogService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.jwtProperties = jwtProperties;
        this.bootstrapProperties = bootstrapProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.currentUserService = currentUserService;
        this.userSessionService = userSessionService;
        this.userPreferenceService = userPreferenceService;
    }

    @Override
    @Transactional
    public CurrentUserVO bootstrapAdmin(BootstrapAdminDTO dto, HttpServletRequest request) {
        if (!StringUtils.hasText(bootstrapProperties.getToken())) {
            throw new ForbiddenException("未配置初始化令牌");
        }
        if (!bootstrapProperties.getToken().equals(dto.getBootstrapToken())) {
            throw new ForbiddenException("初始化令牌无效");
        }
        if (userMapper.selectCount(null) > 0) {
            throw new DuplicateResourceException("系统已存在用户，不能再次初始化管理员");
        }
        RoleEntity adminRole = ensureAdminRole();
        UserEntity user = new UserEntity();
        user.setUserNo(generateNo("U"));
        user.setUsername(dto.getUsername());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setEmail(dto.getEmail());
        user.setStatus(1);
        userMapper.insert(user);
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setUserId(user.getId());
        userRole.setRoleId(adminRole.getId());
        userRoleMapper.insert(userRole);
        recordLogin(user.getId(), user.getUsername(), "SUCCESS", "bootstrap-admin");
        return toCurrentUserVO(currentUserService.load(user.getId()));
    }

    @Override
    @Transactional
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        UserEntity user =
                userMapper.selectOne(
                        new LambdaQueryWrapper<UserEntity>()
                                .eq(UserEntity::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            recordLogin(null, dto.getUsername(), "FAILED", "账号或密码错误");
            throw new UnauthorizedException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLogin(user.getId(), user.getUsername(), "FAILED", "账号已停用");
            throw new UnauthorizedException("账号已停用");
        }
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        CurrentUser currentUser = currentUserService.load(user.getId());
        IssuedToken issuedToken = jwtUtils.generate(currentUser);
        String refreshToken = generateRefreshToken();
        Instant refreshExpiresAt =
                issuedToken.issuedAt().plusSeconds(jwtProperties.getRefreshTokenTtlDays() * 86400);
        userSessionService.create(
                user.getId(), issuedToken, hashRefreshToken(refreshToken), refreshExpiresAt, request);
        LoginVO vo = buildLoginVO(currentUser, issuedToken, refreshToken);
        recordLogin(user.getId(), user.getUsername(), "SUCCESS", null);
        return vo;
    }

    @Override
    @Transactional
    public LoginVO refresh(RefreshSessionDTO dto, HttpServletRequest request) {
        UserSessionEntity session =
                userSessionService.validateRefreshToken(hashRefreshToken(dto.getRefreshToken()));
        CurrentUser currentUser = currentUserService.load(session.getUserId());
        IssuedToken issuedToken = jwtUtils.generate(currentUser);
        String refreshToken = generateRefreshToken();
        Instant refreshExpiresAt =
                issuedToken.issuedAt().plusSeconds(jwtProperties.getRefreshTokenTtlDays() * 86400);
        userSessionService.rotateRefreshToken(
                session,
                issuedToken,
                hashRefreshToken(dto.getRefreshToken()),
                hashRefreshToken(refreshToken),
                refreshExpiresAt);
        return buildLoginVO(currentUser, issuedToken, refreshToken);
    }

    @Override
    public void logout(String authorizationHeader) {
        if (authorizationHeader == null
                || !authorizationHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return;
        }
        String token = authorizationHeader.substring(SecurityConstants.BEARER_PREFIX.length());
        JwtClaims claims = jwtUtils.parse(token);
        userSessionService.revokeByJti(claims.jti(), "logout");
        tokenBlacklistService.blacklist(claims);
    }

    @Override
    public CurrentUserVO currentUser() {
        return toCurrentUserVO(SecurityUtils.currentUser());
    }

    private RoleEntity ensureAdminRole() {
        RoleEntity adminRole =
                roleMapper.selectOne(
                        new LambdaQueryWrapper<RoleEntity>()
                                .eq(RoleEntity::getRoleCode, SecurityConstants.ADMIN_ROLE_CODE));
        if (adminRole != null) {
            return adminRole;
        }
        RoleEntity role = new RoleEntity();
        role.setRoleCode(SecurityConstants.ADMIN_ROLE_CODE);
        role.setRoleName("系统管理员");
        role.setRoleType("system");
        role.setDataScope("all");
        role.setDescription("系统初始化管理员角色");
        role.setSortOrder(1);
        role.setStatus(1);
        roleMapper.insert(role);
        return role;
    }

    private CurrentUserVO toCurrentUserVO(CurrentUser user) {
        CurrentUserVO vo = new CurrentUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setOrganizationId(user.getOrganizationId());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setRoleCodes(user.getRoleCodes());
        vo.setRoleIds(user.getRoleIds());
        vo.setPermissions(user.getPermissions());
        UserEntity userEntity = userMapper.selectById(user.getUserId());
        vo.setAvatarUrl(
                userEntity != null && StringUtils.hasText(userEntity.getAvatarUrl())
                        ? "/api/me/profile/avatar/content"
                        : null);
        vo.setMustChangePassword(
                userEntity != null && Boolean.TRUE.equals(userEntity.getMustChangePassword()));
        return vo;
    }

    private LoginVO buildLoginVO(CurrentUser currentUser, IssuedToken issuedToken, String refreshToken) {
        LoginVO vo = new LoginVO();
        vo.setAccessToken(issuedToken.accessToken());
        vo.setRefreshToken(refreshToken);
        vo.setExpiresIn(jwtProperties.getAccessTokenTtlMinutes() * 60);
        vo.setRefreshExpiresIn(jwtProperties.getRefreshTokenTtlDays() * 86400);
        vo.setUser(toCurrentUserVO(currentUser));
        vo.setPreferredLandingPath(userPreferenceService.preferredLandingPath(currentUser));
        UserEntity user = userMapper.selectById(currentUser.getUserId());
        vo.setMustChangePassword(user != null && Boolean.TRUE.equals(user.getMustChangePassword()));
        return vo;
    }

    private String generateRefreshToken() {
        byte[] value = new byte[32];
        SECURE_RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hashRefreshToken(String refreshToken) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available", exception);
        }
    }

    private void recordLogin(Long userId, String username, String result, String failReason) {
        loginLogService.record(userId, username, result, failReason);
    }

    private String generateNo(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
    }
}
