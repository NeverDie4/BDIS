package com.bdis.modules.research.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.service.ProjectMaterialService;
import com.bdis.modules.research.vo.ProjectMaterialVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProjectMaterialControllerTest {
    private MockMvc mockMvc;
    @Mock private ProjectMaterialService materialService;
    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectMaterialController(materialService, authorizationService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void materialOperationsUseDedicatedPermissions() throws Exception {
        when(materialService.list(10L, null)).thenReturn(List.of(new ProjectMaterialVO()));
        when(materialService.bind(org.mockito.ArgumentMatchers.eq(10L), any())).thenReturn(30L);
        mockMvc.perform(get("/research-projects/10/materials")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        mockMvc.perform(post("/research-projects/10/materials").contentType("application/json").content(
                        "{\"fileId\":20,\"fileUsage\":\"document\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/research-projects/10/materials/20")).andExpect(status().isOk());
        verify(authorizationService).requirePermission("research:project-material:list");
        verify(authorizationService).requirePermission("research:project-material:add");
        verify(authorizationService).requirePermission("research:project-material:delete");
    }

    @Test
    void materialListPermissionFailureReturns403() throws Exception {
        doThrow(new ForbiddenException("no permission")).when(authorizationService)
                .requirePermission("research:project-material:list");
        mockMvc.perform(get("/research-projects/10/materials")).andExpect(status().isForbidden());
    }

    @Test
    void invalidMaterialRequestFailsValidation() throws Exception {
        mockMvc.perform(post("/research-projects/10/materials").contentType("application/json")
                        .content("{\"fileId\":20,\"fileUsage\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
