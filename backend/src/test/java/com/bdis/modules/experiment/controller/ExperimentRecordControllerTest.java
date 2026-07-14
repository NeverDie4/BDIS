package com.bdis.modules.experiment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.experiment.service.ExperimentRecordService;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.permission.service.AuthorizationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ExperimentRecordControllerTest {

    @Mock private ExperimentRecordService recordService;
    @Mock private AuthorizationService authorizationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(
                                new ExperimentRecordController(recordService, authorizationService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void pageChecksListPermission() throws Exception {
        when(recordService.page(any()))
                .thenReturn(new PageResult<>(List.of(new ExperimentRecordListVO()), 1, 10, 1));

        mockMvc.perform(get("/experiment-records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        verify(authorizationService).requirePermission("edu:experiment-record:list");
    }

    @Test
    void detailChecksDetailPermissionAndValidatesId() throws Exception {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setId(1L);
        when(recordService.getDetail(1L)).thenReturn(vo);

        mockMvc.perform(get("/experiment-records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
        verify(authorizationService).requirePermission("edu:experiment-record:detail");

        mockMvc.perform(get("/experiment-records/0")).andExpect(status().isBadRequest());
    }

    @Test
    void createChecksAddPermissionAndReturnsDetail() throws Exception {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setId(10L);
        when(recordService.create(any())).thenReturn(10L);
        when(recordService.getDetail(10L)).thenReturn(vo);

        mockMvc.perform(
                        post("/experiment-records")
                                .contentType("application/json")
                                .content(
                                        "{\"recordNo\":\"EXP-1\",\"courseId\":2,"
                                                + "\"experimentTitle\":\"Title\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
        verify(authorizationService).requirePermission("edu:experiment-record:add");
    }

    @Test
    void createRejectsMissingOrMultipleSourceAtRequestValidation() throws Exception {
        mockMvc.perform(
                        post("/experiment-records")
                                .contentType("application/json")
                                .content("{\"recordNo\":\"EXP-1\",\"experimentTitle\":\"Title\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/experiment-records")
                                .contentType("application/json")
                                .content(
                                        "{\"recordNo\":\"EXP-1\",\"courseId\":2,\"projectId\":3,"
                                                + "\"experimentTitle\":\"Title\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateChecksUpdatePermissionAndRejectsForbiddenFields() throws Exception {
        when(recordService.getDetail(1L)).thenReturn(new ExperimentRecordDetailVO());
        mockMvc.perform(
                        put("/experiment-records/1")
                                .contentType("application/json")
                                .content("{\"experimentTitle\":\"Updated\",\"version\":0}"))
                .andExpect(status().isOk());
        verify(authorizationService).requirePermission("edu:experiment-record:update");

        mockMvc.perform(
                        put("/experiment-records/1")
                                .contentType("application/json")
                                .content(
                                        "{\"experimentTitle\":\"Updated\",\"recordNo\":\"OTHER\","
                                                + "\"version\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteChecksDeletePermission() throws Exception {
        mockMvc.perform(delete("/experiment-records/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        verify(authorizationService).requirePermission("edu:experiment-record:delete");
        verify(recordService).delete(1L);
    }

    @Test
    void eachEndpointReturns403WhenPermissionIsDenied() throws Exception {
        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:list");
        mockMvc.perform(get("/experiment-records")).andExpect(status().isForbidden());

        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:detail");
        mockMvc.perform(get("/experiment-records/1")).andExpect(status().isForbidden());

        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:add");
        mockMvc.perform(
                        post("/experiment-records")
                                .contentType("application/json")
                                .content(
                                        "{\"recordNo\":\"EXP-1\",\"courseId\":2,"
                                                + "\"experimentTitle\":\"Title\"}"))
                .andExpect(status().isForbidden());

        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:update");
        mockMvc.perform(
                        put("/experiment-records/1")
                                .contentType("application/json")
                                .content("{\"experimentTitle\":\"Updated\",\"version\":0}"))
                .andExpect(status().isForbidden());

        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:delete");
        mockMvc.perform(delete("/experiment-records/1")).andExpect(status().isForbidden());
    }

    @Test
    void submitUsesDedicatedPathPermissionAndUnifiedResponse() throws Exception {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setArchiveStatus("submitted");
        when(recordService.getDetail(1L)).thenReturn(vo);

        mockMvc.perform(
                        post("/experiment-records/1/submit")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archiveStatus").value("submitted"));
        verify(authorizationService).requirePermission("edu:experiment-record:submit");
        verify(recordService).submit(any(), any());
    }

    @Test
    void archiveUsesDedicatedPathPermissionAndUnifiedResponse() throws Exception {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setArchiveStatus("archived");
        when(recordService.getDetail(1L)).thenReturn(vo);

        mockMvc.perform(
                        post("/experiment-records/1/archive")
                                .contentType("application/json")
                                .content("{\"version\":0,\"archiveComment\":\"reviewed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.archiveStatus").value("archived"));
        verify(authorizationService).requirePermission("edu:experiment-record:archive");
        verify(recordService).archive(any(), any());
    }

    @Test
    void gradeUsesDedicatedPathPermissionAndReturnsUpdatedDetail() throws Exception {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setId(1L);
        vo.setScore(new java.math.BigDecimal("92.5"));
        when(recordService.getDetail(1L)).thenReturn(vo);

        mockMvc.perform(
                        post("/experiment-records/1/grade")
                                .contentType("application/json")
                                .content(
                                        "{\"version\":0,\"score\":92.5,"
                                                + "\"gradeComment\":\"过程完整\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(92.5));
        verify(authorizationService).requirePermission("edu:experiment-record:grade");
        verify(recordService).grade(any(), any());
    }

    @Test
    void workflowEndpointsValidateIdAndVersion() throws Exception {
        mockMvc.perform(
                        post("/experiment-records/0/submit")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/experiment-records/1/submit")
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/experiment-records/1/archive")
                                .contentType("application/json")
                                .content("{\"version\":-1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void workflowPermissionsAreIndependentAndConflictsMapTo409() throws Exception {
        doThrow(new ForbiddenException("no submit permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:submit");
        mockMvc.perform(
                        post("/experiment-records/1/submit")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isForbidden());

        doThrow(new ForbiddenException("no archive permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-record:archive");
        mockMvc.perform(
                        post("/experiment-records/1/archive")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.reset(authorizationService);
        doThrow(new BusinessException(ResultCodeEnum.CONFLICT, "state conflict"))
                .when(recordService)
                .submit(any(), any());
        mockMvc.perform(
                        post("/experiment-records/1/submit")
                                .contentType("application/json")
                                .content("{\"version\":0}"))
                .andExpect(status().isConflict());
    }

    @Test
    void attachmentListUsesDocumentedPathPermissionAndUnifiedResponse() throws Exception {
        FileResourceVO file = new FileResourceVO();
        file.setId(21L);
        when(recordService.listAttachments(1L, "image")).thenReturn(List.of(file));

        mockMvc.perform(get("/experiment-records/1/attachments").param("fileUsage", "image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(21));
        verify(authorizationService).requirePermission("edu:experiment-attachment:list");
    }

    @Test
    void attachmentBindUsesDocumentedPathPermissionAndValidatesBody() throws Exception {
        FileBusinessVO relation = new FileBusinessVO();
        relation.setId(31L);
        when(recordService.bindAttachment(any(), any())).thenReturn(relation);

        mockMvc.perform(
                        post("/experiment-records/1/attachments")
                                .contentType("application/json")
                                .content(
                                        "{\"fileId\":21,\"fileUsage\":\"attachment\","
                                                + "\"sortOrder\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(31));
        verify(authorizationService).requirePermission("edu:experiment-attachment:add");

        mockMvc.perform(
                        post("/experiment-records/1/attachments")
                                .contentType("application/json")
                                .content("{\"fileUsage\":\"attachment\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(
                        post("/experiment-records/1/attachments")
                                .contentType("application/json")
                                .content("{\"fileId\":21,\"fileUsage\":\"other\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachmentUnbindUsesDocumentedPathPermissionAndValidatesIds() throws Exception {
        mockMvc.perform(delete("/experiment-records/1/attachments/21")).andExpect(status().isOk());
        verify(authorizationService).requirePermission("edu:experiment-attachment:delete");
        verify(recordService).unbindAttachment(1L, 21L);

        mockMvc.perform(delete("/experiment-records/0/attachments/21"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/experiment-records/1/attachments/0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attachmentPermissionsAreIndependentAndServiceErrorsUseGlobalMapping() throws Exception {
        doThrow(new ForbiddenException("no list permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-attachment:list");
        mockMvc.perform(get("/experiment-records/1/attachments")).andExpect(status().isForbidden());

        org.mockito.Mockito.reset(authorizationService);
        doThrow(new BusinessException(ResultCodeEnum.CONFLICT, "record is read-only"))
                .when(recordService)
                .bindAttachment(any(), any());
        mockMvc.perform(
                        post("/experiment-records/1/attachments")
                                .contentType("application/json")
                                .content("{\"fileId\":21,\"fileUsage\":\"image\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void attachmentWritePermissionsCannotSubstituteForEachOther() throws Exception {
        doThrow(new ForbiddenException("no add permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-attachment:add");
        mockMvc.perform(
                        post("/experiment-records/1/attachments")
                                .contentType("application/json")
                                .content("{\"fileId\":21,\"fileUsage\":\"attachment\"}"))
                .andExpect(status().isForbidden());

        org.mockito.Mockito.reset(authorizationService);
        doThrow(new ForbiddenException("no delete permission"))
                .when(authorizationService)
                .requirePermission("edu:experiment-attachment:delete");
        mockMvc.perform(delete("/experiment-records/1/attachments/21"))
                .andExpect(status().isForbidden());
    }
}
