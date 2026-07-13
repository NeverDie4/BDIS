package com.bdis.modules.research.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
import com.bdis.modules.research.service.ResearchProjectService;
import com.bdis.modules.research.vo.ResearchProjectDetailVO;
import com.bdis.modules.research.vo.ResearchProjectListVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ResearchProjectControllerTest {

    private MockMvc mockMvc;
    @Mock private ResearchProjectService projectService;
    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResearchProjectController(projectService, authorizationService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listChecksPermissionAndReturnsPage() throws Exception {
        ResearchProjectListVO vo = new ResearchProjectListVO();
        vo.setProjectNo("P-001");
        when(projectService.page(any())).thenReturn(new PageResult<>(List.of(vo), 1, 10, 1));

        mockMvc.perform(get("/research-projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records[0].projectNo").value("P-001"));
        verify(authorizationService).requirePermission("research:project:list");
    }

    @Test
    void createReturnsDetailAndChecksPermission() throws Exception {
        ResearchProjectDetailVO vo = new ResearchProjectDetailVO();
        vo.setId(10L);
        when(projectService.create(any())).thenReturn(10L);
        when(projectService.getDetail(10L)).thenReturn(vo);

        mockMvc.perform(post("/research-projects").contentType("application/json").content(
                        "{\"projectNo\":\"P-001\",\"projectName\":\"Research\",\"projectType\":\"research\",\"leaderId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(10));
        verify(authorizationService).requirePermission("research:project:add");
    }

    @Test
    void missingRequiredCreateFieldReturnsValidationError() throws Exception {
        mockMvc.perform(post("/research-projects").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void forbiddenProjectListIsReturnedAs403() throws Exception {
        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService).requirePermission("research:project:list");

        mockMvc.perform(get("/research-projects")).andExpect(status().isForbidden());
    }

    @Test
    void updateUsesProjectUpdatePermission() throws Exception {
        when(projectService.getDetail(10L)).thenReturn(new ResearchProjectDetailVO());
        mockMvc.perform(put("/research-projects/10").contentType("application/json").content(
                        "{\"projectName\":\"Updated\",\"projectType\":\"research\"}"))
                .andExpect(status().isOk());
        verify(authorizationService).requirePermission("research:project:update");
    }

    @Test
    void leaderChangeAndStatusChangeUseDedicatedPermissions() throws Exception {
        when(projectService.getDetail(10L)).thenReturn(new ResearchProjectDetailVO());
        mockMvc.perform(post("/research-projects/10/leader").contentType("application/json").content(
                        "{\"newLeaderId\":8,\"reason\":\"handover\",\"version\":0}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/research-projects/10/status").contentType("application/json").content(
                        "{\"targetStatus\":\"ongoing\",\"reason\":\"start\",\"version\":0}"))
                .andExpect(status().isOk());
        verify(authorizationService).requirePermission("research:project:update");
        verify(authorizationService).requirePermission("research:project:status");
    }

    @Test
    void leaderChangePermissionFailureReturns403() throws Exception {
        doThrow(new ForbiddenException("no permission")).when(authorizationService)
                .requirePermission("research:project:update");
        mockMvc.perform(post("/research-projects/10/leader").contentType("application/json").content(
                        "{\"newLeaderId\":8,\"reason\":\"handover\",\"version\":0}"))
                .andExpect(status().isForbidden());
    }
}
