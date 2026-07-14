package com.bdis.modules.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.modules.course.service.ExperimentStepService;
import com.bdis.modules.course.vo.ExperimentStepVO;
import com.bdis.modules.permission.service.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExperimentStepControllerTest {

    private MockMvc mockMvc;

    @Mock private ExperimentStepService stepService;

    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new ExperimentStepController(stepService, authorizationService))
                        .build();
    }

    @Test
    void createStepReturnsUnifiedResultAndChecksPermission() throws Exception {
        ExperimentStepVO vo = new ExperimentStepVO();
        vo.setId(21L);
        vo.setCourseId(11L);
        vo.setStepNo("S-01");
        when(stepService.create(any(), any())).thenReturn(vo);

        mockMvc.perform(
                        post("/courses/11/steps")
                                .contentType("application/json")
                                .content("{\"stepNo\":\"S-01\",\"stepTitle\":\"Prepare sample\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.courseId").value(11))
                .andExpect(jsonPath("$.data.stepNo").value("S-01"));

        verify(authorizationService).requirePermission("edu:course-step:save");
    }
}
