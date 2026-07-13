package com.bdis.modules.research.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.request.ProjectMemberAddRequest;
import com.bdis.modules.research.service.ProjectMemberService;
import com.bdis.modules.research.vo.ProjectMemberVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ProjectMemberControllerTest {

    private MockMvc mockMvc;
    @Mock private ProjectMemberService memberService;
    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProjectMemberController(memberService, authorizationService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void memberListUsesMemberListPermission() throws Exception {
        when(memberService.list(10L, null)).thenReturn(List.of(new ProjectMemberVO()));
        mockMvc.perform(get("/research-projects/10/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        verify(authorizationService).requirePermission("research:project-member:list");
    }

    @Test
    void addUpdateAndRemoveUseDedicatedPermissions() throws Exception {
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(20L);
        when(memberService.add(org.mockito.ArgumentMatchers.eq(10L), any(ProjectMemberAddRequest.class))).thenReturn(20L);
        when(memberService.get(10L, 7L)).thenReturn(vo);

        mockMvc.perform(post("/research-projects/10/members").contentType("application/json").content(
                        "{\"userId\":7,\"memberRole\":\"student\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/research-projects/10/members/7").contentType("application/json").content(
                        "{\"memberRole\":\"researcher\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/research-projects/10/members/7"))
                .andExpect(status().isOk());

        verify(authorizationService).requirePermission("research:project-member:add");
        verify(authorizationService).requirePermission("research:project-member:update");
        verify(authorizationService).requirePermission("research:project-member:remove");
    }
}
