package com.bdis.modules.training.controller;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.service.TrainingRecordService;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TrainingRecordControllerTest {
    @Mock TrainingRecordService service;
    @Mock AuthorizationService authorization;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new TrainingRecordController(service, authorization))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void allFiveRoutesUseDedicatedPermissionsIncludingSafeRemove() throws Exception {
        when(service.page(any())).thenReturn(new PageResult<>(List.of(new TrainingRecordListVO()),1,10,1));
        TrainingRecordDetailVO detail = new TrainingRecordDetailVO();
        detail.setId(1L);
        when(service.getDetail(anyLong())).thenReturn(detail);
        when(service.create(any())).thenReturn(1L);

        mvc.perform(get("/training-records")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-record:list");
        mvc.perform(get("/training-records/1")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-record:detail");
        mvc.perform(post("/training-records").contentType("application/json")
                        .content("{\"planId\":1,\"userId\":8}")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-record:add");
        mvc.perform(put("/training-records/1").contentType("application/json")
                        .content("{\"trainingStatus\":\"learning\"}")).andExpect(status().isOk());
        verify(authorization).requirePermission("edu:training-record:update");
        mvc.perform(delete("/training-records/1")).andExpect(status().isOk());
        verify(authorization, times(2)).requirePermission("edu:training-record:update");
    }

    @Test
    void eachPermissionDenialReturns403() throws Exception {
        String[] permissions = {
            "edu:training-record:list","edu:training-record:detail",
            "edu:training-record:add","edu:training-record:update","edu:training-record:update"
        };
        var requests = List.of(
                get("/training-records"), get("/training-records/1"),
                post("/training-records").contentType("application/json").content("{\"planId\":1,\"userId\":8}"),
                put("/training-records/1").contentType("application/json").content("{\"trainingStatus\":\"learning\"}"),
                delete("/training-records/1")
        );
        for (int i=0;i<permissions.length;i++) {
            reset(authorization);
            doThrow(new ForbiddenException("denied")).when(authorization).requirePermission(permissions[i]);
            mvc.perform(requests.get(i)).andExpect(status().isForbidden());
        }
    }

    @Test
    void validationRejectsInvalidIdBadRangesAndImmutableFields() throws Exception {
        mvc.perform(get("/training-records/0")).andExpect(status().isBadRequest());
        mvc.perform(post("/training-records").contentType("application/json").content("{\"planId\":1}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/training-records/1").contentType("application/json")
                        .content("{\"progress\":101}")).andExpect(status().isBadRequest());
        mvc.perform(put("/training-records/1").contentType("application/json")
                        .content("{\"planId\":2}")).andExpect(status().isBadRequest());
    }
}
