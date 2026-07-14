package com.bdis.modules.training.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingMaterialService;
import com.bdis.modules.training.vo.TrainingMaterialDetailVO;
import com.bdis.modules.training.vo.TrainingMaterialListVO;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingMaterialControllerTest {
    @Mock TrainingMaterialService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc =
                MockMvcBuilders.standaloneSetup(
                                new TrainingMaterialController(service, authorization))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void allFiveRoutesUseDedicatedPermissions() throws Exception {
        when(service.page(any()))
                .thenReturn(new PageResult<>(List.of(new TrainingMaterialListVO()), 1, 10, 1));
        TrainingMaterialDetailVO detail = new TrainingMaterialDetailVO();
        detail.setId(1L);
        when(service.getDetail(anyLong())).thenReturn(detail);
        when(service.create(any())).thenReturn(1L);
        mvc.perform(get("/training-materials")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:list");
        mvc.perform(get("/training-materials/1")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:detail");
        mvc.perform(
                        post("/training-materials")
                                .contentType("application/json")
                                .content(createJson()))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:add");
        mvc.perform(
                        put("/training-materials/1")
                                .contentType("application/json")
                                .content(updateJson()))
                .andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:update");
        mvc.perform(delete("/training-materials/1")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-material:delete");
    }

    @Test
    void permissionDenialsReturn403() throws Exception {
        String[] permissions = {
            "edu:training-material:list",
            "edu:training-material:detail",
            "edu:training-material:add",
            "edu:training-material:update",
            "edu:training-material:delete"
        };
        var requests =
                List.of(
                        get("/training-materials"),
                        get("/training-materials/1"),
                        post("/training-materials")
                                .contentType("application/json")
                                .content(createJson()),
                        put("/training-materials/1")
                                .contentType("application/json")
                                .content(updateJson()),
                        delete("/training-materials/1"));
        for (int i = 0; i < permissions.length; i++) {
            reset(authorization);
            doThrow(new ForbiddenException("denied"))
                    .when(authorization)
                    .requirePermission(permissions[i]);
            mvc.perform(requests.get(i)).andExpect(status().isForbidden());
        }
    }

    @Test
    void validationRejectsBadIdsBodyAndImmutableNumber() throws Exception {
        mvc.perform(get("/training-materials/0")).andExpect(status().isBadRequest());
        mvc.perform(post("/training-materials").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put("/training-materials/1")
                                .contentType("application/json")
                                .content(
                                        updateJson()
                                                .replace(
                                                        "\"version\":0",
                                                        "\"version\":0,\"materialNo\":\"OTHER\"")))
                .andExpect(status().isBadRequest());
    }

    private String createJson() {
        return "{\"materialNo\":\"MAT\",\"materialName\":\"Material\",\"materialType\":\"video\",\"fileId\":10,\"sourceType\":\"upload\"}";
    }

    private String updateJson() {
        return "{\"materialName\":\"Material\",\"materialType\":\"video\",\"fileId\":10,\"sourceType\":\"upload\",\"status\":1,\"version\":0}";
    }
}
