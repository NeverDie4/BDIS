package com.bdis.modules.training.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingRecordService;
import com.bdis.modules.training.vo.TrainingParticipantBatchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingParticipantControllerTest {
    @Mock TrainingRecordService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TrainingParticipantController(service, authorization))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void batchRouteUsesAddPermission() throws Exception {
        when(service.batchCreate(eq(1L), any())).thenReturn(new TrainingParticipantBatchResultVO());
        mvc.perform(post("/training-plans/1/participants/batch")
                        .contentType("application/json").content("{\"userIds\":[8,9]}"))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-record:add");
    }

    @Test
    void denialAndInvalidArgumentsAreRejected() throws Exception {
        doThrow(new ForbiddenException("denied"))
                .when(authorization).requirePermission("edu:training-record:add");
        mvc.perform(post("/training-plans/1/participants/batch")
                        .contentType("application/json").content("{\"userIds\":[8]}"))
                .andExpect(status().isForbidden());
        reset(authorization);
        mvc.perform(post("/training-plans/0/participants/batch")
                        .contentType("application/json").content("{\"userIds\":[]}"))
                .andExpect(status().isBadRequest());
    }
}
