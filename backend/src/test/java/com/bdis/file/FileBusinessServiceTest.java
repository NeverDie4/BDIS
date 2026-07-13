package com.bdis.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.impl.FileBusinessServiceImpl;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import java.time.LocalDateTime;
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
class FileBusinessServiceTest {

    @Mock private FileBusinessMapper businessMapper;
    @Mock private FileResourceMapper resourceMapper;
    @Mock private BusinessReferenceValidator referenceValidator;
    @Mock private AuditLogService auditLogService;

    private FileBusinessService service;

    @BeforeEach
    void setUp() {
        service =
                new FileBusinessServiceImpl(
                        businessMapper, resourceMapper, referenceValidator, auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", "n/a"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bindValidFilePersistsSortOrderAndRecordsSingleM05Audit() {
        when(resourceMapper.selectById(21L)).thenReturn(activeFile(21L));
        when(businessMapper.insert(any(FileBusinessEntity.class))).thenAnswer(invocation -> {
            FileBusinessEntity entity = invocation.getArgument(0);
            entity.setId(31L);
            return 1;
        });

        var result = service.bind(bindDTO());

        assertEquals(31L, result.getId());
        assertEquals(10, result.getSortOrder());
        ArgumentCaptor<FileBusinessEntity> entity =
                ArgumentCaptor.forClass(FileBusinessEntity.class);
        verify(businessMapper).insert(entity.capture());
        assertEquals(10, entity.getValue().getSortOrder());
        ArgumentCaptor<AuditRecordDTO> audit = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(audit.capture());
        assertEquals("M05_FILE", audit.getValue().getOperationModule());
        assertEquals("BIND", audit.getValue().getOperationType());
    }

    @Test
    void bindRejectsDisabledFileBeforeCreatingRelation() {
        FileResourceEntity disabled = activeFile(21L);
        disabled.setStatus(0);
        when(resourceMapper.selectById(21L)).thenReturn(disabled);

        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.bind(bindDTO()))
                        .getResultCode());
        verify(businessMapper, never()).insert(any(FileBusinessEntity.class));
    }

    @Test
    void duplicateBindRemainsIdempotentAndDoesNotWriteSecondAudit() {
        when(resourceMapper.selectById(21L)).thenReturn(activeFile(21L));
        FileBusinessEntity existing = relation(31L, 21L, "attachment");
        when(businessMapper.selectOne(any())).thenReturn(existing);

        assertEquals(31L, service.bind(bindDTO()).getId());

        verify(businessMapper, never()).insert(any(FileBusinessEntity.class));
        verify(auditLogService, never()).record(any());
    }

    @Test
    void listByBusinessUsageLoadsFilesInOneBatchAndPreservesRelationOrder() {
        FileBusinessEntity second = relation(32L, 22L, "image");
        second.setSortOrder(20);
        FileBusinessEntity first = relation(31L, 21L, "image");
        first.setSortOrder(10);
        when(businessMapper.selectList(any())).thenReturn(List.of(second, first));
        when(resourceMapper.selectByIds(any()))
                .thenReturn(List.of(activeFile(21L), activeFile(22L)));

        var files = service.listByBusiness("edu_experiment_record", 1L, "image");

        assertEquals(List.of(21L, 22L), files.stream().map(v -> v.getId()).toList());
        verify(resourceMapper).selectByIds(any());
        verify(resourceMapper, never()).selectById(any());
    }

    @Test
    void businessExistenceCheckUsesRelationCountWithoutLoadingFiles() {
        when(businessMapper.selectCount(any())).thenReturn(2L);

        assertTrue(service.existsByBusiness("edu_experiment_record", 1L));

        verify(resourceMapper, never()).selectByIds(any());
        verify(resourceMapper, never()).selectById(any());
    }

    @Test
    void exactUnbindDeletesOnlyMatchingRelationAndRecordsAudit() {
        when(businessMapper.selectOne(any()))
                .thenReturn(relation(31L, 21L, "attachment"));

        service.unbind("edu_experiment_record", 1L, 21L);

        verify(businessMapper).deleteById(31L);
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void exactUnbindRejectsMissingRelationAndAuditFailureIsNotSwallowed() {
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.unbind("edu_experiment_record", 1L, 21L));

        when(businessMapper.selectOne(any()))
                .thenReturn(relation(31L, 21L, "attachment"));
        doThrow(new IllegalStateException("audit failed"))
                .when(auditLogService)
                .record(any(AuditRecordDTO.class));
        assertThrows(
                IllegalStateException.class,
                () -> service.unbind("edu_experiment_record", 1L, 21L));
    }

    private FileBusinessBindDTO bindDTO() {
        FileBusinessBindDTO dto = new FileBusinessBindDTO();
        dto.setFileId(21L);
        dto.setBizType("edu_experiment_record");
        dto.setBizId(1L);
        dto.setFileUsage("attachment");
        dto.setSortOrder(10);
        dto.setRemark("record attachment");
        return dto;
    }

    private FileResourceEntity activeFile(Long id) {
        FileResourceEntity file = new FileResourceEntity();
        file.setId(id);
        file.setFileName("file-" + id);
        file.setStatus(1);
        file.setIsDeleted(0);
        return file;
    }

    private FileBusinessEntity relation(Long id, Long fileId, String usage) {
        FileBusinessEntity entity = new FileBusinessEntity();
        entity.setId(id);
        entity.setFileId(fileId);
        entity.setBizType("edu_experiment_record");
        entity.setBizId(1L);
        entity.setFileUsage(usage);
        entity.setSortOrder(0);
        entity.setCreatedAt(LocalDateTime.now());
        return entity;
    }
}
