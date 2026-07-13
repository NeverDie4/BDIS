package com.bdis.modules.research.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.service.ResearchAchievementService;
import com.bdis.modules.research.vo.ResearchAchievementDetailVO;
import com.bdis.modules.research.vo.ResearchAchievementListVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ResearchAchievementControllerTest {
    private MockMvc mockMvc;
    @Mock private ResearchAchievementService service;
    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ResearchAchievementController(service, authorizationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void achievementRoutesUseDedicatedPermissions() throws Exception {
        when(service.page(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        when(service.getDetail(10L)).thenReturn(new ResearchAchievementDetailVO());
        when(service.create(any())).thenReturn(10L);
        when(service.getDetail(20L)).thenReturn(new ResearchAchievementDetailVO());

        mockMvc.perform(get("/research-achievements")).andExpect(status().isOk());
        mockMvc.perform(get("/research-achievements/10")).andExpect(status().isOk());
        mockMvc.perform(post("/research-achievements").contentType("application/json")
                        .content("{\"achievementNo\":\"A-001\",\"projectId\":1,\"achievementName\":\"Paper\",\"achievementType\":\"paper\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/research-achievements/20").contentType("application/json")
                        .content("{\"achievementName\":\"Updated\",\"achievementType\":\"paper\",\"achievementStatus\":\"draft\",\"version\":0}"))
                .andExpect(status().isOk());
    }

    @Test
    void listPermissionFailureReturns403() throws Exception {
        doThrow(new ForbiddenException("no permission")).when(authorizationService)
                .requirePermission("research:achievement:list");
        mockMvc.perform(get("/research-achievements")).andExpect(status().isForbidden());
    }

    @Test
    void invalidPathAndBodyAreRejected() throws Exception {
        mockMvc.perform(post("/research-achievements").contentType("application/json")
                        .content("{\"achievementNo\":\"\",\"projectId\":null}"))
                .andExpect(status().isBadRequest());
    }
}
