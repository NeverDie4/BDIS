package com.bdis.modules.training.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingFeedbackService;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingFeedbackControllerTest {
    @Mock TrainingFeedbackService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new TrainingFeedbackController(service, authorization))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void listCreateAndUpdateUseDedicatedPermissions() throws Exception {
        when(service.page(any())).thenReturn(new PageResult<>(List.of(), 1, 10, 0));
        TrainingFeedbackDetailVO detail = new TrainingFeedbackDetailVO();
        detail.setId(1L);
        when(service.create(any())).thenReturn(1L);
        when(service.getDetail(1L)).thenReturn(detail);

        mvc.perform(get("/training-feedbacks?planId=1&rating=5")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-feedback:list");
        mvc.perform(
                        post("/training-feedbacks")
                                .contentType("application/json")
                                .content("{\"trainingRecordId\":1,\"rating\":5}"))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-feedback:add");
        mvc.perform(
                        put("/training-feedbacks/1")
                                .contentType("application/json")
                                .content("{\"rating\":4}"))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-feedback:update");
    }

    @Test
    void permissionDenialsAre403AndValidationIs400() throws Exception {
        String[] permissions = {
            "edu:training-feedback:list",
            "edu:training-feedback:add",
            "edu:training-feedback:update"
        };
        var requests =
                List.of(
                        get("/training-feedbacks"),
                        post("/training-feedbacks")
                                .contentType("application/json")
                                .content("{\"trainingRecordId\":1,\"rating\":5}"),
                        put("/training-feedbacks/1")
                                .contentType("application/json")
                                .content("{\"rating\":4}"));
        for (int i = 0; i < permissions.length; i++) {
            reset(authorization);
            doThrow(new ForbiddenException("denied"))
                    .when(authorization)
                    .requirePermission(permissions[i]);
            mvc.perform(requests.get(i)).andExpect(status().isForbidden());
        }
        reset(authorization);
        mvc.perform(
                        post("/training-feedbacks")
                                .contentType("application/json")
                                .content("{\"trainingRecordId\":1,\"rating\":6}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/training-feedbacks/0")
                                .contentType("application/json")
                                .content("{\"rating\":4}"))
                .andExpect(status().isBadRequest());
    }
}
