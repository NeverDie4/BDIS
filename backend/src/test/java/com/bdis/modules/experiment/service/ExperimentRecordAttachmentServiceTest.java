package com.bdis.modules.experiment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.request.ExperimentRecordAttachmentBindRequest;
import com.bdis.modules.experiment.service.impl.ExperimentRecordServiceImpl;
import com.bdis.modules.file.vo.FileResourceVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ExperimentRecordAttachmentServiceTest {

    @Mock private ExperimentRecordMapper recordMapper;
    @Mock private UserMapper userMapper;
    @Mock private FileBusinessService fileBusinessService;
    @Mock private AuditLogService auditLogService;

    private ExperimentRecordService service;

    @BeforeEach
    void setUp() {
        service =
                new ExperimentRecordServiceImpl(
                        recordMapper, userMapper, fileBusinessService, auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", "n/a"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void attachmentsCanBeListedForEveryWorkflowStateWithoutPerFileQueries() {
        FileResourceVO file = new FileResourceVO();
        file.setId(21L);
        when(fileBusinessService.listByBusiness(
                        "edu_experiment_record", 1L, "attachment"))
                .thenReturn(List.of(file));

        for (String status :
                List.of(
                        ExperimentArchiveStatus.DRAFT,
                        ExperimentArchiveStatus.SUBMITTED,
                        ExperimentArchiveStatus.ARCHIVED)) {
            when(recordMapper.selectById(1L)).thenReturn(record(status));
            assertEquals(21L, service.listAttachments(1L, "attachment").getFirst().getId());
        }

        verify(fileBusinessService, org.mockito.Mockito.times(3))
                .listByBusiness("edu_experiment_record", 1L, "attachment");
    }

    @Test
    void attachmentListReturnsEmptyAndRejectsMissingRecord() {
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        when(fileBusinessService.listByBusiness("edu_experiment_record", 1L, null))
                .thenReturn(List.of());
        assertEquals(List.of(), service.listAttachments(1L, null));

        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.listAttachments(99L, null))
                        .getResultCode());
        verify(fileBusinessService, never())
                .listByBusiness("edu_experiment_record", 99L, null);
    }

    @Test
    void draftAttachmentBindUsesServerControlledBusinessReference() {
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        FileBusinessVO relation = new FileBusinessVO();
        relation.setId(31L);
        when(fileBusinessService.bind(any())).thenReturn(relation);

        FileBusinessVO result = service.bindAttachment(1L, bindRequest());

        assertEquals(31L, result.getId());
        ArgumentCaptor<FileBusinessBindDTO> captor =
                ArgumentCaptor.forClass(FileBusinessBindDTO.class);
        verify(fileBusinessService).bind(captor.capture());
        assertEquals(21L, captor.getValue().getFileId());
        assertEquals("edu_experiment_record", captor.getValue().getBizType());
        assertEquals(1L, captor.getValue().getBizId());
        assertEquals("image", captor.getValue().getFileUsage());
        assertEquals(10, captor.getValue().getSortOrder());
    }

    @Test
    void attachmentBindRejectsMissingSubmittedAndArchivedRecordsBeforeM05() {
        assertThrows(BusinessException.class, () -> service.bindAttachment(99L, bindRequest()));

        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.SUBMITTED));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.bindAttachment(1L, bindRequest()))
                        .getResultCode());

        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.ARCHIVED));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.bindAttachment(1L, bindRequest()))
                        .getResultCode());
        verify(fileBusinessService, never()).bind(any());
    }

    @Test
    void attachmentBindRejectsInvalidCurrentUserAndPropagatesM05Failure() {
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(
                                BusinessException.class,
                                () -> service.bindAttachment(1L, bindRequest()))
                        .getResultCode());

        UserEntity disabled = activeUser();
        disabled.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.bindAttachment(1L, bindRequest()))
                        .getResultCode());

        when(userMapper.selectById(7L)).thenReturn(activeUser());
        when(fileBusinessService.bind(any()))
                .thenThrow(new BusinessException(ResultCodeEnum.CONFLICT, "duplicate"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.bindAttachment(1L, bindRequest()))
                        .getResultCode());
    }

    @Test
    void draftAttachmentUnbindUsesExactBusinessAndFileIdentity() {
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        when(userMapper.selectById(7L)).thenReturn(activeUser());

        service.unbindAttachment(1L, 21L);

        verify(fileBusinessService).unbind("edu_experiment_record", 1L, 21L);
    }

    @Test
    void attachmentUnbindRejectsNonDraftMissingAndInvalidOperator() {
        assertThrows(BusinessException.class, () -> service.unbindAttachment(99L, 21L));
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.ARCHIVED));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.unbindAttachment(1L, 21L))
                        .getResultCode());

        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(
                                BusinessException.class,
                                () -> service.unbindAttachment(1L, 21L))
                        .getResultCode());
        verify(fileBusinessService, never()).unbind(eq("edu_experiment_record"), eq(1L), eq(21L));
    }

    @Test
    void attachmentUnbindDoesNotSwallowM05Failure() {
        when(recordMapper.selectById(1L)).thenReturn(record(ExperimentArchiveStatus.DRAFT));
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        org.mockito.Mockito.doThrow(new IllegalStateException("m05 failed"))
                .when(fileBusinessService)
                .unbind("edu_experiment_record", 1L, 21L);

        assertThrows(IllegalStateException.class, () -> service.unbindAttachment(1L, 21L));
    }

    private ExperimentRecordAttachmentBindRequest bindRequest() {
        ExperimentRecordAttachmentBindRequest request =
                new ExperimentRecordAttachmentBindRequest();
        request.setFileId(21L);
        request.setFileUsage("image");
        request.setSortOrder(10);
        request.setRemark("现场图片");
        return request;
    }

    private ExperimentRecordEntity record(String status) {
        ExperimentRecordEntity entity = new ExperimentRecordEntity();
        entity.setId(1L);
        entity.setArchiveStatus(status);
        entity.setVersion(0);
        return entity;
    }

    private UserEntity activeUser() {
        UserEntity user = new UserEntity();
        user.setId(7L);
        user.setStatus(1);
        user.setIsDeleted(0);
        return user;
    }
}
