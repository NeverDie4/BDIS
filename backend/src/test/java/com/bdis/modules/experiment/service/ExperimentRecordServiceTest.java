package com.bdis.modules.experiment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.file.service.FileBusinessService;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.constant.ExperimentArchiveStatus;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.mapper.ExperimentRecordMapper;
import com.bdis.modules.experiment.query.ExperimentRecordQuery;
import com.bdis.modules.experiment.request.ExperimentRecordArchiveRequest;
import com.bdis.modules.experiment.request.ExperimentRecordCreateRequest;
import com.bdis.modules.experiment.request.ExperimentRecordGradeRequest;
import com.bdis.modules.experiment.request.ExperimentRecordSubmitRequest;
import com.bdis.modules.experiment.request.ExperimentRecordUpdateRequest;
import com.bdis.modules.experiment.service.impl.ExperimentRecordServiceImpl;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class ExperimentRecordServiceTest {

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
    void pageReturnsJoinedSummaryAndForwardsFilters() {
        ExperimentRecordListVO vo = new ExperimentRecordListVO();
        vo.setId(1L);
        vo.setSourceType("course");
        ExperimentRecordQuery query = new ExperimentRecordQuery();
        query.setKeyword("EXP");
        query.setCourseId(2L);
        query.setRecorderId(7L);
        query.setArchiveStatus("draft");
        query.setRecordedFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        query.setRecordedTo(LocalDateTime.of(2026, 12, 31, 23, 59));
        when(recordMapper.selectPageVO(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<ExperimentRecordListVO> page = invocation.getArgument(0);
                            page.setRecords(List.of(vo));
                            page.setTotal(1);
                            return page;
                        });

        var result = service.page(query);

        assertEquals(1, result.getTotal());
        assertEquals("course", result.getRecords().getFirst().getSourceType());
        verify(recordMapper).selectPageVO(any(), org.mockito.ArgumentMatchers.same(query));
    }

    @Test
    void pageRejectsInvalidRecordedRange() {
        ExperimentRecordQuery query = new ExperimentRecordQuery();
        query.setRecordedFrom(LocalDateTime.of(2026, 2, 1, 0, 0));
        query.setRecordedTo(LocalDateTime.of(2026, 1, 1, 0, 0));

        assertThrows(BusinessException.class, () -> service.page(query));
        verify(recordMapper, never()).selectPageVO(any(), any());
    }

    @Test
    void detailReturnsCourseOrProjectSummary() {
        ExperimentRecordDetailVO vo = new ExperimentRecordDetailVO();
        vo.setId(1L);
        vo.setCourseId(2L);
        vo.setSourceType("course");
        when(recordMapper.selectDetailById(1L)).thenReturn(vo);

        ExperimentRecordDetailVO result = service.getDetail(1L);

        assertEquals("course", result.getSourceType());
        assertEquals(2L, result.getCourseId());
    }

    @Test
    void detailRejectsMissingRecord() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDetail(99L))
                        .getResultCode());
    }

    @Test
    void otherUserCannotReadOrOperateAnotherUsersRecordButAdminCan() {
        ExperimentRecordEntity record = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        ExperimentRecordDetailVO detail = new ExperimentRecordDetailVO();
        detail.setId(1L);
        detail.setRecorderId(7L);
        detail.setCourseId(2L);
        when(recordMapper.selectDetailById(1L)).thenReturn(detail);
        setUser(8L, "TEACHER");

        assertThrows(ForbiddenException.class, () -> service.getDetail(1L));
        assertThrows(ForbiddenException.class, () -> service.update(1L, validUpdate()));
        assertThrows(ForbiddenException.class, () -> service.listAttachments(1L, null));

        setUser(99L, "ADMIN");
        when(recordMapper.selectDetailById(1L)).thenReturn(detail);
        assertNotNull(service.getDetail(1L));
    }

    @Test
    void activeProjectMemberCanReadAnotherMembersRecord() {
        when(recordMapper.selectProjectByIdIncludingDeleted(3L))
                .thenReturn(activeProject(3L, "ongoing"));
        when(recordMapper.existsActiveProjectMember(3L, 8L)).thenReturn(true);

        ExperimentRecordDetailVO detail = new ExperimentRecordDetailVO();
        detail.setId(1L);
        detail.setRecorderId(7L);
        detail.setProjectId(3L);
        when(recordMapper.selectDetailById(1L)).thenReturn(detail);
        setUser(8L, "TEACHER");

        assertNotNull(service.getDetail(1L));
    }

    @Test
    void recordListPassesCurrentUserScopeToMapperQuery() {
        setUser(8L, "TEACHER");
        when(recordMapper.selectPageVO(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.page(new ExperimentRecordQuery());

        ArgumentCaptor<ExperimentRecordQuery> captor =
                ArgumentCaptor.forClass(ExperimentRecordQuery.class);
        verify(recordMapper).selectPageVO(any(), captor.capture());
        assertEquals(8L, captor.getValue().getScopeUserId());
        assertEquals(Boolean.FALSE, captor.getValue().getScopeAll());
    }

    private void setUser(Long id, String role) {
        CurrentUser user =
                new CurrentUser(
                        id,
                        "user-" + id,
                        "User",
                        null,
                        null,
                        Set.of(role),
                        Set.of(),
                        Set.of("edu:experiment-record:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    @Test
    void createCourseRecordUsesCurrentUserAndDraftWorkflowFields() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setCourseId(2L);
        CourseEntity course = activeCourse(2L);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(course);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.insert(any(ExperimentRecordEntity.class)))
                .thenAnswer(
                        invocation -> {
                            ExperimentRecordEntity entity = invocation.getArgument(0);
                            entity.setId(11L);
                            return 1;
                        });

        Long id = service.create(request);

        assertEquals(11L, id);
        ArgumentCaptor<ExperimentRecordEntity> captor =
                ArgumentCaptor.forClass(ExperimentRecordEntity.class);
        verify(recordMapper).insert(captor.capture());
        ExperimentRecordEntity saved = captor.getValue();
        assertEquals(7L, saved.getRecorderId());
        assertEquals(ExperimentArchiveStatus.DRAFT, saved.getArchiveStatus());
        assertNull(saved.getSubmittedAt());
        assertNull(saved.getSubmittedBy());
        assertNull(saved.getArchivedAt());
        assertNull(saved.getArchivedBy());
        assertNull(saved.getArchiveComment());
        assertNotNull(saved.getRecordedAt());
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void createProjectRecordSucceedsForActiveNonCompletedProject() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setProjectId(3L);
        when(recordMapper.selectProjectByIdIncludingDeleted(3L))
                .thenReturn(activeProject(3L, "ongoing"));
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.insert(any(ExperimentRecordEntity.class)))
                .thenAnswer(
                        invocation -> {
                            ((ExperimentRecordEntity) invocation.getArgument(0)).setId(12L);
                            return 1;
                        });

        assertEquals(12L, service.create(request));
    }

    @Test
    void createRejectsEmptyOrMultipleSources() {
        ExperimentRecordCreateRequest empty = validCreate();
        assertThrows(BusinessException.class, () -> service.create(empty));

        ExperimentRecordCreateRequest both = validCreate();
        both.setCourseId(2L);
        both.setProjectId(3L);
        assertThrows(BusinessException.class, () -> service.create(both));
    }

    @Test
    void createRejectsMissingDeletedOrDisabledCourse() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setCourseId(2L);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        CourseEntity deleted = activeCourse(2L);
        deleted.setIsDeleted(1);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        CourseEntity disabled = activeCourse(2L);
        disabled.setStatus(0);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(disabled);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());
    }

    @Test
    void createRejectsMissingDeletedOrCompletedProject() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setProjectId(3L);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        ResearchProjectEntity deleted = activeProject(3L, "ongoing");
        deleted.setIsDeleted(1);
        when(recordMapper.selectProjectByIdIncludingDeleted(3L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        when(recordMapper.selectProjectByIdIncludingDeleted(3L))
                .thenReturn(activeProject(3L, "completed"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());
    }

    @Test
    void createRejectsMissingOrDisabledRecorder() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setCourseId(2L);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));

        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        UserEntity disabled = activeUser(7L);
        disabled.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());
    }

    @Test
    void createRejectsDuplicateIncludingDeletedAndConcurrentUniqueConflict() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setCourseId(2L);
        when(recordMapper.selectByRecordNoIncludingDeleted("EXP-001"))
                .thenReturn(new ExperimentRecordEntity());
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        when(recordMapper.selectByRecordNoIncludingDeleted("EXP-001")).thenReturn(null);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.insert(any(ExperimentRecordEntity.class)))
                .thenThrow(new DuplicateKeyException("uk_record_no"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());
    }

    @Test
    void createRejectsBlankTitle() {
        ExperimentRecordCreateRequest request = validCreate();
        request.setCourseId(2L);
        request.setExperimentTitle("  ");
        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void updateDraftChangesOnlyMutableFieldsAndAudits() {
        ExperimentRecordEntity entity = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.updateById(entity)).thenReturn(1);
        ExperimentRecordUpdateRequest request = validUpdate();

        service.update(1L, request);

        assertEquals("Updated title", entity.getExperimentTitle());
        assertEquals("EXP-001", entity.getRecordNo());
        assertEquals(2L, entity.getCourseId());
        assertEquals(7L, entity.getRecorderId());
        assertEquals("draft", entity.getArchiveStatus());
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void updateRejectsMissingNonDraftAndVersionConflict() {
        ExperimentRecordUpdateRequest request = validUpdate();
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.update(99L, request))
                        .getResultCode());

        ExperimentRecordEntity submitted = draftRecord();
        submitted.setArchiveStatus("submitted");
        when(recordMapper.selectById(1L)).thenReturn(submitted);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());

        ExperimentRecordEntity archived = draftRecord();
        archived.setArchiveStatus("archived");
        when(recordMapper.selectById(1L)).thenReturn(archived);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());

        ExperimentRecordEntity stale = draftRecord();
        stale.setVersion(2);
        when(recordMapper.selectById(1L)).thenReturn(stale);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());
    }

    @Test
    void updateRejectsOptimisticLockMiss() {
        ExperimentRecordEntity entity = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.updateById(entity)).thenReturn(0);

        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, validUpdate()))
                        .getResultCode());
    }

    @Test
    void deleteDraftUsesLogicalDeleteAndAuditsWithoutDeletingFiles() {
        ExperimentRecordEntity entity = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(fileBusinessService.existsByBusiness("edu_experiment_record", 1L)).thenReturn(false);
        when(recordMapper.logicalDeleteByIdAndVersion(
                        anyLong(), any(Integer.class), anyLong(), any(LocalDateTime.class)))
                .thenReturn(1);

        service.delete(1L);

        verify(recordMapper)
                .logicalDeleteByIdAndVersion(eq(1L), eq(0), eq(7L), any(LocalDateTime.class));
        verify(fileBusinessService, never()).deleteByFileId(anyLong());
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void deleteRejectsMissingNonDraftOrAttachments() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.delete(99L)).getResultCode());

        ExperimentRecordEntity submitted = draftRecord();
        submitted.setArchiveStatus("submitted");
        when(recordMapper.selectById(1L)).thenReturn(submitted);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.delete(1L)).getResultCode());

        ExperimentRecordEntity draft = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(draft);
        when(fileBusinessService.existsByBusiness("edu_experiment_record", 1L)).thenReturn(true);
        BusinessException exception =
                assertThrows(BusinessException.class, () -> service.delete(1L));
        assertEquals(ResultCodeEnum.CONFLICT, exception.getResultCode());
        assertTrue(exception.getMessage().contains("attachment"));
    }

    @Test
    void deleteAttachmentExistenceFailureIsNotSwallowed() {
        ExperimentRecordEntity draft = draftRecord();
        when(recordMapper.selectById(1L)).thenReturn(draft);
        when(fileBusinessService.existsByBusiness("edu_experiment_record", 1L))
                .thenThrow(new IllegalStateException("m05 unavailable"));

        assertThrows(IllegalStateException.class, () -> service.delete(1L));
        verify(recordMapper, never())
                .logicalDeleteByIdAndVersion(anyLong(), any(), anyLong(), any());
    }

    @Test
    void submitDraftUsesAtomicTransitionAndRecordsAudit() {
        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.submitByIdAndVersion(eq(1L), eq(0), eq(7L), any(LocalDateTime.class)))
                .thenReturn(1);

        service.submit(1L, submitRequest(0));

        verify(recordMapper).submitByIdAndVersion(eq(1L), eq(0), eq(7L), any(LocalDateTime.class));
        ArgumentCaptor<AuditRecordDTO> audit = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(audit.capture());
        assertEquals("SUBMIT", audit.getValue().getOperationType());
        assertEquals("M14_EXPERIMENT_RECORD", audit.getValue().getOperationModule());
        assertEquals("edu_experiment_record", audit.getValue().getBizType());
        assertEquals(1L, audit.getValue().getBizId());
    }

    @Test
    void submitRejectsSubmittedArchivedAndVersionConflict() {
        ExperimentRecordEntity submitted = completeDraftRecord();
        submitted.setArchiveStatus(ExperimentArchiveStatus.SUBMITTED);
        when(recordMapper.selectById(1L)).thenReturn(submitted);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity archived = completeDraftRecord();
        archived.setArchiveStatus(ExperimentArchiveStatus.ARCHIVED);
        when(recordMapper.selectById(1L)).thenReturn(archived);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity stale = completeDraftRecord();
        stale.setVersion(2);
        when(recordMapper.selectById(1L)).thenReturn(stale);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());
    }

    @Test
    void submitRejectsMissingRecordAndAtomicRace() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.submit(99L, submitRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.submitByIdAndVersion(eq(1L), eq(0), eq(7L), any(LocalDateTime.class)))
                .thenReturn(0);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());
    }

    @Test
    void submitRejectsIncompleteTitleProcessResultOrRecordedTime() {
        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        entity.setExperimentProcess(" ");
        assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)));
        entity.setExperimentProcess("process");
        entity.setExperimentResult(null);
        assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)));
        entity.setExperimentResult("result");
        entity.setExperimentTitle(" ");
        assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)));
        entity.setExperimentTitle("title");
        entity.setRecordedAt(null);
        assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)));
        verify(recordMapper, never())
                .submitByIdAndVersion(anyLong(), any(), anyLong(), any(LocalDateTime.class));
    }

    @Test
    void submitRejectsDeletedDisabledOrMissingCurrentUser() {
        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());

        UserEntity deleted = activeUser(7L);
        deleted.setIsDeleted(1);
        when(userMapper.selectById(7L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());

        UserEntity disabled = activeUser(7L);
        disabled.setStatus(0);
        when(userMapper.selectById(7L)).thenReturn(disabled);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());
    }

    @Test
    void submitRejectsDeletedOrDisabledSource() {
        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        CourseEntity deleted = activeCourse(2L);
        deleted.setIsDeleted(1);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());

        CourseEntity disabled = activeCourse(2L);
        disabled.setStatus(0);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(disabled);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.submit(1L, submitRequest(0)))
                        .getResultCode());
    }

    @Test
    void submitDoesNotSwallowAuditFailure() {
        ExperimentRecordEntity entity = completeDraftRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(activeCourse(2L));
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.submitByIdAndVersion(anyLong(), any(), anyLong(), any())).thenReturn(1);
        doThrow(new IllegalStateException("audit failed"))
                .when(auditLogService)
                .record(any(AuditRecordDTO.class));

        assertThrows(IllegalStateException.class, () -> service.submit(1L, submitRequest(0)));
    }

    @Test
    void archiveSubmittedUsesAtomicTransitionAndPreservesSubmission() {
        ExperimentRecordEntity entity = submittedRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.archiveByIdAndVersion(
                        eq(1L), eq(0), eq(7L), any(LocalDateTime.class), eq("reviewed")))
                .thenReturn(1);

        ExperimentRecordArchiveRequest request = archiveRequest(0);
        request.setArchiveComment("reviewed");
        service.archive(1L, request);

        verify(recordMapper)
                .archiveByIdAndVersion(
                        eq(1L), eq(0), eq(7L), any(LocalDateTime.class), eq("reviewed"));
        assertNotNull(entity.getSubmittedAt());
        assertEquals(8L, entity.getSubmittedBy());
        ArgumentCaptor<AuditRecordDTO> audit = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(audit.capture());
        assertEquals("ARCHIVE", audit.getValue().getOperationType());
    }

    @Test
    void archiveRejectsDraftArchivedAndVersionConflict() {
        when(recordMapper.selectById(1L)).thenReturn(completeDraftRecord());
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity archived = submittedRecord();
        archived.setArchiveStatus(ExperimentArchiveStatus.ARCHIVED);
        when(recordMapper.selectById(1L)).thenReturn(archived);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity stale = submittedRecord();
        stale.setVersion(2);
        when(recordMapper.selectById(1L)).thenReturn(stale);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());
    }

    @Test
    void archiveRejectsMissingSubmissionMetadataOrRecord() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.archive(99L, archiveRequest(0)))
                        .getResultCode());

        ExperimentRecordEntity entity = submittedRecord();
        entity.setSubmittedAt(null);
        when(recordMapper.selectById(1L)).thenReturn(entity);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());
        entity.setSubmittedAt(LocalDateTime.now());
        entity.setSubmittedBy(null);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());
    }

    @Test
    void archiveRejectsInvalidCurrentUserAndAtomicRace() {
        ExperimentRecordEntity entity = submittedRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());

        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.archiveByIdAndVersion(
                        eq(1L), eq(0), eq(7L), any(LocalDateTime.class), any()))
                .thenReturn(0);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.archive(1L, archiveRequest(0)))
                        .getResultCode());
    }

    @Test
    void archiveDoesNotSwallowAuditFailure() {
        ExperimentRecordEntity entity = submittedRecord();
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(userMapper.selectById(7L)).thenReturn(activeUser(7L));
        when(recordMapper.archiveByIdAndVersion(anyLong(), any(), anyLong(), any(), any()))
                .thenReturn(1);
        doThrow(new IllegalStateException("audit failed"))
                .when(auditLogService)
                .record(any(AuditRecordDTO.class));

        assertThrows(IllegalStateException.class, () -> service.archive(1L, archiveRequest(0)));
    }

    @Test
    void gradeSubmittedRecordPersistsScoreAndReviewer() {
        ExperimentRecordEntity entity = submittedRecord();
        CourseEntity course = activeCourse(2L);
        course.setTeacherId(8L);
        setUser(8L, "TEACHER");
        when(recordMapper.selectById(1L)).thenReturn(entity);
        when(recordMapper.selectCourseByIdIncludingDeleted(2L)).thenReturn(course);
        when(userMapper.selectById(8L)).thenReturn(activeUser(8L));
        when(recordMapper.gradeByIdAndVersion(anyLong(), any(), anyLong(), any(), any(), any()))
                .thenReturn(1);

        ExperimentRecordGradeRequest request = new ExperimentRecordGradeRequest();
        request.setVersion(0);
        request.setScore(new BigDecimal("92.5"));
        request.setGradeComment("过程完整");

        service.grade(1L, request);

        verify(recordMapper)
                .gradeByIdAndVersion(
                        eq(1L), eq(0), eq(8L), eq(new BigDecimal("92.5")), any(), eq("过程完整"));
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void gradeRejectsUnsubmittedRecord() {
        when(recordMapper.selectById(1L)).thenReturn(draftRecord());

        ExperimentRecordGradeRequest request = new ExperimentRecordGradeRequest();
        request.setVersion(0);
        request.setScore(BigDecimal.TEN);

        assertThrows(BusinessException.class, () -> service.grade(1L, request));
        verify(recordMapper, never()).gradeByIdAndVersion(anyLong(), any(), anyLong(), any(), any(), any());
    }

    private ExperimentRecordCreateRequest validCreate() {
        ExperimentRecordCreateRequest request = new ExperimentRecordCreateRequest();
        request.setRecordNo("EXP-001");
        request.setExperimentTitle(" Experiment title ");
        return request;
    }

    private ExperimentRecordUpdateRequest validUpdate() {
        ExperimentRecordUpdateRequest request = new ExperimentRecordUpdateRequest();
        request.setExperimentTitle("Updated title");
        request.setExperimentProcess("process");
        request.setExperimentResult("result");
        request.setVersion(0);
        return request;
    }

    private ExperimentRecordSubmitRequest submitRequest(Integer version) {
        ExperimentRecordSubmitRequest request = new ExperimentRecordSubmitRequest();
        request.setVersion(version);
        return request;
    }

    private ExperimentRecordArchiveRequest archiveRequest(Integer version) {
        ExperimentRecordArchiveRequest request = new ExperimentRecordArchiveRequest();
        request.setVersion(version);
        return request;
    }

    private ExperimentRecordEntity draftRecord() {
        ExperimentRecordEntity entity = new ExperimentRecordEntity();
        entity.setId(1L);
        entity.setRecordNo("EXP-001");
        entity.setCourseId(2L);
        entity.setRecorderId(7L);
        entity.setArchiveStatus("draft");
        entity.setVersion(0);
        return entity;
    }

    private ExperimentRecordEntity completeDraftRecord() {
        ExperimentRecordEntity entity = draftRecord();
        entity.setExperimentTitle("title");
        entity.setExperimentProcess("process");
        entity.setExperimentResult("result");
        entity.setRecordedAt(LocalDateTime.now());
        return entity;
    }

    private ExperimentRecordEntity submittedRecord() {
        ExperimentRecordEntity entity = completeDraftRecord();
        entity.setArchiveStatus(ExperimentArchiveStatus.SUBMITTED);
        entity.setSubmittedAt(LocalDateTime.now().minusHours(1));
        entity.setSubmittedBy(8L);
        return entity;
    }

    private CourseEntity activeCourse(Long id) {
        CourseEntity entity = new CourseEntity();
        entity.setId(id);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }

    private ResearchProjectEntity activeProject(Long id, String status) {
        ResearchProjectEntity entity = new ResearchProjectEntity();
        entity.setId(id);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setProjectStatus(status);
        return entity;
    }

    private UserEntity activeUser(Long id) {
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }
}
