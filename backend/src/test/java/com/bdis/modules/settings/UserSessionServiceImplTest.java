package com.bdis.modules.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.bdis.common.security.IssuedToken;
import com.bdis.common.exception.UnauthorizedException;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.mapper.UserSessionMapper;
import com.bdis.modules.settings.service.impl.UserSessionServiceImpl;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceImplTest {

    @Mock private UserSessionMapper sessionMapper;

    @Mock private UserMapper userMapper;

    @Mock private TokenBlacklistService tokenBlacklistService;

    @Mock private HttpServletRequest request;

    @Test
    void samePersistentDeviceReplacesPreviousActiveSession() {
        initializeTableMetadata();
        UserSessionServiceImpl service =
                new UserSessionServiceImpl(sessionMapper, userMapper, tokenBlacklistService);
        UserSessionEntity previous = new UserSessionEntity();
        previous.setId(1L);
        previous.setUserId(7L);
        previous.setTokenJti("old-jti");
        previous.setSessionStatus("active");
        previous.setIssuedAt(LocalDateTime.now().minusHours(1));
        previous.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(request.getHeader("X-Device-Id")).thenReturn("bdis_0123456789abcdef0123456789abcdef");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla Chrome Linux");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(sessionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(previous));
        Instant issuedAt = Instant.now();
        IssuedToken token =
                new IssuedToken("token", "new-jti", issuedAt, issuedAt.plusSeconds(3600));

        Instant refreshExpiresAt = issuedAt.plusSeconds(86400);

        service.create(7L, token, "refresh-hash", refreshExpiresAt, request);

        assertThat(previous.getSessionStatus()).isEqualTo("revoked");
        assertThat(previous.getRevokeReason()).isEqualTo("replaced_by_new_login");
        verify(sessionMapper).updateById(previous);
        verify(tokenBlacklistService).blacklist(any());
        ArgumentCaptor<UserSessionEntity> inserted =
                ArgumentCaptor.forClass(UserSessionEntity.class);
        verify(sessionMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getDeviceId())
                .isEqualTo("bdis_0123456789abcdef0123456789abcdef");
        assertThat(inserted.getValue().getTokenJti()).isEqualTo("new-jti");
        assertThat(inserted.getValue().getRefreshTokenHash()).isEqualTo("refresh-hash");
        assertThat(inserted.getValue().getRefreshExpiresAt())
                .isEqualTo(LocalDateTime.ofInstant(refreshExpiresAt, java.time.ZoneId.systemDefault()));
        verify(sessionMapper).update(eq(null), any(LambdaUpdateWrapper.class));
    }

    @Test
    void activeRefreshTokenCanBeValidatedAndRotated() {
        initializeTableMetadata();
        UserSessionServiceImpl service =
                new UserSessionServiceImpl(sessionMapper, userMapper, tokenBlacklistService);
        UserSessionEntity session = new UserSessionEntity();
        session.setId(2L);
        session.setUserId(7L);
        session.setTokenJti("old-jti");
        session.setRefreshTokenHash("old-refresh-hash");
        session.setSessionStatus("active");
        session.setRefreshExpiresAt(LocalDateTime.now().plusDays(1));
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus(1);
        when(sessionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(session);
        when(userMapper.selectById(7L)).thenReturn(user);
        when(sessionMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(1);

        UserSessionEntity validated = service.validateRefreshToken("old-refresh-hash");
        Instant issuedAt = Instant.now();
        Instant refreshExpiresAt = issuedAt.plusSeconds(86400);
        IssuedToken token =
                new IssuedToken("token", "new-jti", issuedAt, issuedAt.plusSeconds(3600));
        service.rotateRefreshToken(
                validated, token, "old-refresh-hash", "new-refresh-hash", refreshExpiresAt);

        assertThat(validated).isSameAs(session);
        assertThat(session.getTokenJti()).isEqualTo("new-jti");
        assertThat(session.getRefreshTokenHash()).isEqualTo("new-refresh-hash");
        assertThat(session.getRefreshExpiresAt())
                .isEqualTo(LocalDateTime.ofInstant(refreshExpiresAt, java.time.ZoneId.systemDefault()));
        verify(sessionMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        verify(sessionMapper, never()).updateById(session);
    }

    @Test
    void repeatedRefreshTokenConsumptionFailsWhenAtomicUpdateMisses() {
        initializeTableMetadata();
        UserSessionServiceImpl service =
                new UserSessionServiceImpl(sessionMapper, userMapper, tokenBlacklistService);
        UserSessionEntity session = new UserSessionEntity();
        session.setId(2L);
        session.setUserId(7L);
        session.setRefreshTokenHash("old-refresh-hash");
        session.setSessionStatus("active");
        Instant issuedAt = Instant.now();
        IssuedToken token =
                new IssuedToken("token", "new-jti", issuedAt, issuedAt.plusSeconds(3600));
        when(sessionMapper.update(eq(null), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(
                        () ->
                                service.rotateRefreshToken(
                                        session,
                                        token,
                                        "old-refresh-hash",
                                        "new-refresh-hash",
                                        issuedAt.plusSeconds(86400)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void elapsedSessionCleanupUsesRefreshExpiryWhenRefreshTokenExists() {
        initializeTableMetadata();
        UserSessionServiceImpl service =
                new UserSessionServiceImpl(sessionMapper, userMapper, tokenBlacklistService);
        when(request.getHeader("X-Device-Id")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla Chrome Linux");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        Instant issuedAt = Instant.now().minusSeconds(3600);
        IssuedToken token =
                new IssuedToken("token", "new-jti", issuedAt, issuedAt.minusSeconds(60));

        service.create(7L, token, "refresh-hash", Instant.now().plusSeconds(86400), request);

        verify(sessionMapper).update(eq(null), any(LambdaUpdateWrapper.class));
        ArgumentCaptor<UserSessionEntity> inserted =
                ArgumentCaptor.forClass(UserSessionEntity.class);
        verify(sessionMapper).insert(inserted.capture());
        assertThat(inserted.getValue().getSessionStatus()).isEqualTo("active");
    }

    private void initializeTableMetadata() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(configuration, UserSessionEntity.class.getName());
        assistant.setCurrentNamespace(UserSessionEntity.class.getName());
        TableInfoHelper.initTableInfo(assistant, UserSessionEntity.class);
    }
}
