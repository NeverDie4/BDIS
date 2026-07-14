package com.bdis.modules.training.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingSummaryService;
import com.bdis.modules.training.vo.TrainingSummaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingSummaryControllerTest {
    @Mock TrainingSummaryService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new TrainingSummaryController(service, authorization))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void summaryUsesDedicatedPermission() throws Exception {
        TrainingSummaryVO vo = new TrainingSummaryVO();
        vo.setPlanId(1L);
        when(service.getSummary(1L)).thenReturn(vo);
        mvc.perform(get("/training-plans/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").value(1));
        verify(authorization).requirePermission("edu:training-summary:view");
    }

    @Test
    void denialIs403AndInvalidIdIs400() throws Exception {
        doThrow(new ForbiddenException("denied"))
                .when(authorization)
                .requirePermission("edu:training-summary:view");
        mvc.perform(get("/training-plans/1/summary")).andExpect(status().isForbidden());
        reset(authorization);
        mvc.perform(get("/training-plans/0/summary")).andExpect(status().isBadRequest());
    }
}
