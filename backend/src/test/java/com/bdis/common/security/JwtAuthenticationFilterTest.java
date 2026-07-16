package com.bdis.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.settings.entity.UserSessionEntity;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class JwtAuthenticationFilterTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);

    private final TokenBlacklistService blacklistService = mock(TokenBlacklistService.class);

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);

    private final UserSessionService sessionService = mock(UserSessionService.class);

    private final UserMapper userMapper = mock(UserMapper.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter =
                new JwtAuthenticationFilter(
                        jwtUtils,
                        blacklistService,
                        currentUserService,
                        sessionService,
                        userMapper,
                        objectMapper);
        JwtClaims claims =
                new JwtClaims(1L, "user", "jti-1", Instant.now(), Instant.now().plusSeconds(600));
        when(jwtUtils.parse("token")).thenReturn(claims);
        when(blacklistService.isBlacklisted("jti-1")).thenReturn(false);
        UserSessionEntity session = new UserSessionEntity();
        session.setSessionId("session-1");
        when(sessionService.validate(claims)).thenReturn(session);
        when(currentUserService.load(1L))
                .thenReturn(
                        new CurrentUser(
                                1L,
                                "user",
                                "User",
                                null,
                                null,
                                Set.of("STUDENT"),
                                Set.of(2L),
                                Set.of()));
        UserEntity user = new UserEntity();
        user.setMustChangePassword(true);
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    @Test
    void blocksBusinessRequestUntilPasswordIsChanged() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("GET", "/api/herbs");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(body.get("code").asText()).isEqualTo("PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    void allowsPasswordChangeRequest() throws Exception {
        MockHttpServletRequest request = authenticatedRequest("PUT", "/api/me/password");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest authenticatedRequest(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.addHeader("Authorization", "Bearer token");
        return request;
    }
}
