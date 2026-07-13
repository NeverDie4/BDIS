package com.bdis.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.JwtClaims;
import com.bdis.common.security.JwtAuthenticationFilter;
import com.bdis.common.security.JwtUtils;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.herb.controller.HerbSpeciesController;
import com.bdis.modules.herb.service.HerbSpeciesService;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HerbSpeciesController.class)
@ContextConfiguration(
        classes = {
            HerbSpeciesController.class,
            SecurityConfig.class,
            JwtAuthenticationFilter.class
        })
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Autowired
    private ObjectProvider<FilterRegistrationBean<JwtAuthenticationFilter>>
            jwtFilterRegistrationProvider;

    @MockBean private HerbSpeciesService herbSpeciesService;

    @MockBean private JwtUtils jwtUtils;

    @MockBean private TokenBlacklistService tokenBlacklistService;

    @MockBean private CurrentUserService currentUserService;

    @Test
    void herbApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/herb/species/list")).andExpect(status().isUnauthorized());
    }

    @Test
    void bearerTokenEstablishesAuthenticationInsideSecurityFilterChain() throws Exception {
        JwtClaims claims =
                new JwtClaims(
                        1L,
                        "test-user",
                        "test-jti",
                        Instant.now(),
                        Instant.now().plusSeconds(300));
        CurrentUser currentUser =
                new CurrentUser(
                        1L,
                        "test-user",
                        "Test User",
                        null,
                        null,
                        Set.of("TEST"),
                        Set.of(1L),
                        Set.of());
        when(jwtUtils.parse("valid-token")).thenReturn(claims);
        when(tokenBlacklistService.isBlacklisted("test-jti")).thenReturn(false);
        when(currentUserService.load(1L)).thenReturn(currentUser);

        mockMvc.perform(
                        get("/herb/species/list")
                                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void jwtFilterIsNotRegisteredAsAContainerFilter() {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                jwtFilterRegistrationProvider.getIfAvailable();

        assertThat(registration).isNotNull();
        assertThat(registration.isEnabled()).isFalse();
    }

    @Test
    void herbPostApiRequiresAuthentication() throws Exception {
        mockMvc.perform(
                        post("/herb/atlas/feature/batch-extract")
                                .contentType("application/json")
                                .content("{\"speciesId\":null,\"forceRefresh\":false}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mobileHerbApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/mobile/herb/tasks?collectorId=1001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void privateStoragePathRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/files/uploads/missing.png")).andExpect(status().isUnauthorized());
    }

    @Test
    void publicFileEndpointAllowsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/public-files/1/content")).andExpect(status().isNotFound());
    }
}
