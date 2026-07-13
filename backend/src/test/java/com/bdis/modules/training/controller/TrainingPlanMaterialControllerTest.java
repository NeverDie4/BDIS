package com.bdis.modules.training.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingPlanMaterialService;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingPlanMaterialControllerTest {
    @Mock TrainingPlanMaterialService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach void setUp(){
        mvc=MockMvcBuilders.standaloneSetup(new TrainingPlanMaterialController(service,authorization))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test void routesAndPermissionsAreCorrect() throws Exception{
        when(service.list(1L)).thenReturn(List.of(new TrainingPlanMaterialVO()));
        when(service.bind(eq(1L),any())).thenReturn(3L);
        mvc.perform(get("/training-plans/1/materials")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:list");
        mvc.perform(post("/training-plans/1/materials").contentType("application/json").content("{\"materialId\":2}")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:bind");
        mvc.perform(delete("/training-plans/1/materials/2")).andExpect(status().isOk());
        verify(authorization,times(2)).requirePermission("edu:training-material:bind");
    }

    @Test void permissionDenialsAndInvalidIdsAreHandled() throws Exception{
        doThrow(new ForbiddenException("denied")).when(authorization).requirePermission("edu:training-material:list");
        mvc.perform(get("/training-plans/1/materials")).andExpect(status().isForbidden());
        reset(authorization);doThrow(new ForbiddenException("denied")).when(authorization).requirePermission("edu:training-material:bind");
        mvc.perform(post("/training-plans/1/materials").contentType("application/json").content("{\"materialId\":2}")).andExpect(status().isForbidden());
        reset(authorization);doThrow(new ForbiddenException("denied")).when(authorization).requirePermission("edu:training-material:bind");
        mvc.perform(delete("/training-plans/1/materials/2")).andExpect(status().isForbidden());
        mvc.perform(get("/training-plans/0/materials")).andExpect(status().isBadRequest());
        mvc.perform(delete("/training-plans/1/materials/0")).andExpect(status().isBadRequest());
    }
}
