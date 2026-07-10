package com.bdis.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.security.JwtAuthenticationFilter;
import com.bdis.common.security.JwtUtils;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.herb.controller.HerbSpeciesController;
import com.bdis.modules.herb.service.HerbSpeciesService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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

    @MockBean private HerbSpeciesService herbSpeciesService;

    @MockBean private JwtUtils jwtUtils;

    @MockBean private TokenBlacklistService tokenBlacklistService;

    @MockBean private CurrentUserService currentUserService;

    @Test
    void herbApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/herb/species/list")).andExpect(status().isUnauthorized());
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
    void uploadedFilesRemainPubliclyReadable() throws Exception {
        mockMvc.perform(get("/files/uploads/missing.png")).andExpect(status().isNotFound());
    }
}
