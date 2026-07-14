package com.bdis.modules.training.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingPlanService;
import com.bdis.modules.training.vo.TrainingPlanDetailVO;
import com.bdis.modules.training.vo.TrainingPlanListVO;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingPlanControllerTest {
    @Mock TrainingPlanService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(new TrainingPlanController(service, authorization))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void allSevenRoutesUseDedicatedPermissionsAndUnifiedResponses() throws Exception {
        when(service.page(any()))
                .thenReturn(new PageResult<>(List.of(new TrainingPlanListVO()), 1, 10, 1));
        TrainingPlanDetailVO detail = new TrainingPlanDetailVO();
        detail.setId(1L);
        when(service.getDetail(anyLong())).thenReturn(detail);
        when(service.create(any())).thenReturn(1L);

        mvc.perform(get("/training-plans")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-plan:list");
        mvc.perform(get("/training-plans/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        verify(authorization).requirePermission("edu:training-plan:detail");
        mvc.perform(post("/training-plans").contentType("application/json").content(validCreate()))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-plan:add");
        mvc.perform(put("/training-plans/1").contentType("application/json").content(validUpdate()))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-plan:update");
        mvc.perform(delete("/training-plans/1")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-plan:delete");
        mvc.perform(
                        post("/training-plans/1/publish")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-plan:publish");
        mvc.perform(
                        post("/training-plans/1/close")
                                .contentType("application/json")
                                .content("{\"reason\":\"done\",\"version\":0}"))
                .andExpect(status().isOk());
        verify(authorization, times(2)).requirePermission("edu:training-plan:publish");
    }

    @Test
    void eachPermissionDenialReturns403() throws Exception {
        String[] permissions = {
            "edu:training-plan:list", "edu:training-plan:detail", "edu:training-plan:add",
            "edu:training-plan:update", "edu:training-plan:delete", "edu:training-plan:publish",
            "edu:training-plan:publish"
        };
        var requests =
                List.of(
                        get("/training-plans"),
                        get("/training-plans/1"),
                        post("/training-plans")
                                .contentType("application/json")
                                .content(validCreate()),
                        put("/training-plans/1")
                                .contentType("application/json")
                                .content(validUpdate()),
                        delete("/training-plans/1"),
                        post("/training-plans/1/publish")
                                .contentType("application/json")
                                .content("{\"version\":0}"),
                        post("/training-plans/1/close")
                                .contentType("application/json")
                                .content("{\"reason\":\"done\",\"version\":0}"));
        for (int i = 0; i < permissions.length; i++) {
            reset(authorization);
            doThrow(new ForbiddenException("denied"))
                    .when(authorization)
                    .requirePermission(permissions[i]);
            mvc.perform(requests.get(i)).andExpect(status().isForbidden());
        }
    }

    @Test
    void validationRejectsInvalidIdMissingVersionAndImmutableFields() throws Exception {
        mvc.perform(get("/training-plans/0")).andExpect(status().isBadRequest());
        mvc.perform(post("/training-plans/1/publish").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/training-plans/1/close")
                                .contentType("application/json")
                                .content("{\"reason\":\"\",\"version\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/training-plans/1")
                                .contentType("application/json")
                                .content(
                                        validUpdate()
                                                .replace(
                                                        "\"version\":0",
                                                        "\"version\":0,\"planNo\":\"OTHER\"")))
                .andExpect(status().isBadRequest());
    }

    private String validCreate() {
        return "{\"planNo\":\"IT\",\"planName\":\"Plan\",\"planType\":\"course\",\"ownerId\":8}";
    }

    private String validUpdate() {
        return "{\"planName\":\"Plan\",\"planType\":\"course\",\"ownerId\":8,\"version\":0}";
    }
}
