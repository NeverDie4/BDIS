package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingFeedbackMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.query.TrainingRecordQuery;
import com.bdis.modules.training.request.TrainingParticipantBatchRequest;
import com.bdis.modules.training.request.TrainingRecordCreateRequest;
import com.bdis.modules.training.request.TrainingRecordUpdateRequest;
import com.bdis.modules.training.service.impl.TrainingRecordServiceImpl;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import com.bdis.modules.training.vo.TrainingParticipantBatchResultVO;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingRecordServiceTest {
    @Mock TrainingRecordMapper recordMapper;
    @Mock TrainingFeedbackMapper feedbackMapper;
    @Mock TrainingPlanMapper planMapper;
    @Mock UserMapper userMapper;
    @Mock AuditLogService auditLogService;
    TrainingRecordService service;

    @BeforeEach
    void setUp() {
        service =
                new TrainingRecordServiceImpl(
                        recordMapper, feedbackMapper, planMapper, userMapper, auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void learnerCannotReadAnotherLearnersRecordButAdministratorCan() {
        TrainingRecordDetailVO detail = new TrainingRecordDetailVO();
        detail.setId(1L);
        detail.setUserId(7L);
        detail.setPlanId(2L);
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(2L);
        plan.setOwnerId(6L);
        plan.setTrainerId(6L);
        plan.setStatus(1);
        plan.setIsDeleted(0);
        when(recordMapper.selectDetailById(1L)).thenReturn(detail);
        when(planMapper.selectByIdIncludingDeleted(2L)).thenReturn(plan);
        setUser(8L, "STUDENT");

        assertThrows(ForbiddenException.class, () -> service.getDetail(1L));

        setUser(99L, "ADMIN");
        assertNotNull(service.getDetail(1L));
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
                        Set.of("edu:training-record:detail"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, "n/a"));
    }

    @Test
    void joiningTheSamePlanTwiceCreatesTwoAttendanceRecords() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(recordMapper.insert(any(TrainingRecordEntity.class)))
                .thenAnswer(
                        invocation -> {
                            TrainingRecordEntity record = invocation.getArgument(0);
                            record.setId(record.getAttendanceNo().endsWith("0") ? 10L : 11L);
                            return 1;
                        });
        TrainingRecordDetailVO detail = new TrainingRecordDetailVO();
        detail.setUserId(7L);
        detail.setPlanId(1L);
        when(recordMapper.selectDetailById(anyLong())).thenReturn(detail);

        service.join(1L);
        service.join(1L);

        ArgumentCaptor<TrainingRecordEntity> captor =
                ArgumentCaptor.forClass(TrainingRecordEntity.class);
        verify(recordMapper, times(2)).insert(captor.capture());
        assertTrue(
                captor.getAllValues().stream()
                        .allMatch(
                                record ->
                                        "not_started".equals(record.getTrainingStatus())
                                                && BigDecimal.ZERO.compareTo(record.getProgress()) == 0
                                                && record.getCompletedAt() == null));
        assertNotEquals(
                captor.getAllValues().get(0).getAttendanceNo(),
                captor.getAllValues().get(1).getAttendanceNo());
    }

    @Test
    void pageUsesSingleJoinedMapperQueryAndValidatesFilters() {
        TrainingRecordListVO vo = new TrainingRecordListVO();
        vo.setId(1L);
        vo.setPlanName("Plan");
        vo.setRealName("Student");
        when(recordMapper.selectPageVO(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<TrainingRecordListVO> page = invocation.getArgument(0);
                            page.setRecords(List.of(vo));
                            page.setTotal(1);
                            return page;
                        });
        TrainingRecordQuery query = new TrainingRecordQuery();
        query.setKeyword("Student");
        query.setTrainingStatus("learning");
        assertEquals("Student", service.page(query).getRecords().getFirst().getRealName());
        verify(recordMapper, times(1)).selectPageVO(any(), same(query));
        verifyNoInteractions(planMapper, userMapper);

        query.setTrainingStatus("bad");
        assertThrows(BusinessException.class, () -> service.page(query));
        query.setTrainingStatus("learning");
        query.setAttendanceStatus("bad");
        assertThrows(BusinessException.class, () -> service.page(query));
    }

    @Test
    void detailReturnsJoinedSummaryAndMissingIs404() {
        TrainingRecordDetailVO vo = new TrainingRecordDetailVO();
        vo.setId(1L);
        vo.setPlanName("Plan");
        vo.setRealName("Student");
        when(recordMapper.selectDetailById(1L)).thenReturn(vo);
        TrainingFeedbackDetailVO feedback = new TrainingFeedbackDetailVO();
        feedback.setId(3L);
        when(feedbackMapper.selectDetailByRecordId(1L)).thenReturn(feedback);
        TrainingRecordDetailVO result = service.getDetail(1L);
        assertEquals("Plan", result.getPlanName());
        assertEquals(3L, result.getFeedback().getId());
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDetail(2L)).getResultCode());
    }

    @Test
    void detailWithoutFeedbackReturnsNull() {
        TrainingRecordDetailVO vo = new TrainingRecordDetailVO();
        vo.setId(1L);
        when(recordMapper.selectDetailById(1L)).thenReturn(vo);
        assertNull(service.getDetail(1L).getFeedback());
    }

    @Test
    void batchCreateDeduplicatesClassifiesAndUsesBulkReads() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        UserEntity active = user(8L);
        UserEntity disabled = user(9L);
        disabled.setStatus(0);
        UserEntity deleted = user(10L);
        deleted.setIsDeleted(1);
        when(recordMapper.selectUsersIncludingDeleted(anyList()))
                .thenReturn(List.of(active, disabled, deleted));
        TrainingRecordEntity existing = record();
        existing.setUserId(11L);
        when(recordMapper.selectByPlanAndUsers(eq(1L), anyList())).thenReturn(List.of(existing));
        when(recordMapper.insert(any(TrainingRecordEntity.class)))
                .thenAnswer(
                        invocation -> {
                            TrainingRecordEntity entity = invocation.getArgument(0);
                            entity.setId(100L);
                            return 1;
                        });
        TrainingParticipantBatchRequest request = new TrainingParticipantBatchRequest();
        request.setUserIds(new ArrayList<>(List.of(8L, 8L, 9L, 10L, 11L, 12L)));

        TrainingParticipantBatchResultVO result = service.batchCreate(1L, request);

        assertEquals(6, result.getRequestedCount());
        assertEquals(5, result.getUniqueUserCount());
        assertEquals(1, result.getSuccessCount());
        assertEquals(2, result.getDuplicateCount());
        assertEquals(3, result.getFailureCount());
        assertEquals(List.of(8L), result.getSuccessUserIds());
        verify(recordMapper, times(1)).selectUsersIncludingDeleted(anyList());
        verify(recordMapper, times(1)).selectByPlanAndUsers(eq(1L), anyList());
        ArgumentCaptor<TrainingRecordEntity> captor =
                ArgumentCaptor.forClass(TrainingRecordEntity.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals("pending", captor.getValue().getAttendanceStatus());
        assertEquals("not_started", captor.getValue().getTrainingStatus());
        assertEquals(BigDecimal.ZERO, captor.getValue().getProgress());
        verify(auditLogService).record(any());
    }

    @Test
    void batchCreateAllowsDraftButRejectsClosedAndConvertsConcurrentDuplicate() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        when(recordMapper.selectUsersIncludingDeleted(anyList())).thenReturn(List.of(user(8L)));
        when(recordMapper.selectByPlanAndUsers(eq(1L), anyList())).thenReturn(List.of());
        when(recordMapper.insert(any(TrainingRecordEntity.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("dup"));
        TrainingParticipantBatchRequest request = new TrainingParticipantBatchRequest();
        request.setUserIds(List.of(8L));
        TrainingParticipantBatchResultVO result = service.batchCreate(1L, request);
        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getDuplicateCount());

        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.batchCreate(1L, request))
                        .getResultCode());
    }

    @Test
    void removeUsesExactPhysicalDeleteOnlyForPristineRecordsAndAudits() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(recordMapper.selectById(1L)).thenReturn(record());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(feedbackMapper.countByRecordId(1L)).thenReturn(0L);
        when(recordMapper.deletePristine(1L)).thenReturn(1);

        service.remove(1L);

        verify(recordMapper).deletePristine(1L);
        verify(userMapper, never()).deleteById(anyLong());
        verify(auditLogService).record(any());
    }

    @Test
    void removeProtectsProcessFeedbackClosedAndConcurrentChanges() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        TrainingRecordEntity record = record();
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        record.setProgress(BigDecimal.ONE);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.remove(1L)).getResultCode());
        record.setProgress(BigDecimal.ZERO);
        when(feedbackMapper.countByRecordId(1L)).thenReturn(1L);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.remove(1L)).getResultCode());
        when(feedbackMapper.countByRecordId(1L)).thenReturn(0L);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.remove(1L)).getResultCode());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(recordMapper.deletePristine(1L)).thenReturn(0);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.remove(1L)).getResultCode());
    }

    @Test
    void createDefaultsProcessFieldsCopiesCourseAndAudits() {
        TrainingRecordCreateRequest request = createRequest();
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        TrainingPlanEntity plan = plan("draft");
        plan.setCourseId(9L);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan);
        when(recordMapper.insert(any(TrainingRecordEntity.class)))
                .thenAnswer(
                        invocation -> {
                            TrainingRecordEntity entity = invocation.getArgument(0);
                            entity.setId(11L);
                            return 1;
                        });

        assertEquals(11L, service.create(request));
        ArgumentCaptor<TrainingRecordEntity> captor =
                ArgumentCaptor.forClass(TrainingRecordEntity.class);
        verify(recordMapper).insert(captor.capture());
        assertEquals("not_started", captor.getValue().getTrainingStatus());
        assertEquals("pending", captor.getValue().getAttendanceStatus());
        assertEquals(BigDecimal.ZERO, captor.getValue().getProgress());
        assertEquals(9L, captor.getValue().getCourseId());
        verify(auditLogService).record(any());
    }

    @Test
    void createRejectsMissingDeletedClosedUserAndDuplicate() {
        TrainingRecordCreateRequest request = createRequest();
        assertThrows(BusinessException.class, () -> service.create(request));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        assertThrows(BusinessException.class, () -> service.create(request));

        TrainingPlanEntity closed = plan("closed");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(closed);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());

        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        when(recordMapper.selectByPlanAndUser(1L, 8L)).thenReturn(record());
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request))
                        .getResultCode());
    }

    @Test
    void updateProcessFieldsSetsDerivedTimesAndAudits() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(recordMapper.selectById(1L)).thenReturn(record());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(recordMapper.updateById(any(TrainingRecordEntity.class))).thenReturn(1);
        TrainingRecordUpdateRequest request = new TrainingRecordUpdateRequest();
        request.setTrainingStatus("completed");
        request.setAttendanceStatus("present");
        request.setProgress(new BigDecimal("100"));
        request.setScore(new BigDecimal("88"));

        service.update(1L, request);

        ArgumentCaptor<TrainingRecordEntity> captor =
                ArgumentCaptor.forClass(TrainingRecordEntity.class);
        verify(recordMapper).updateById(captor.capture());
        assertNotNull(captor.getValue().getCompletedAt());
        assertNotNull(captor.getValue().getCheckedInAt());
        verify(auditLogService).record(any());
    }

    @Test
    void updateRejectsInvalidStatusRangesClosedAndCompleted() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        TrainingRecordEntity record = record();
        when(recordMapper.selectById(1L)).thenReturn(record);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        TrainingRecordUpdateRequest request = new TrainingRecordUpdateRequest();
        request.setTrainingStatus("bad");
        assertThrows(BusinessException.class, () -> service.update(1L, request));
        request.setTrainingStatus("learning");
        request.setProgress(new BigDecimal("101"));
        assertThrows(BusinessException.class, () -> service.update(1L, request));

        request.setProgress(BigDecimal.TEN);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());

        record.setTrainingStatus("completed");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());
    }

    @Test
    void draftCannotCompleteOrFailButMayRemainLearning() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(recordMapper.selectById(1L)).thenReturn(record());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        TrainingRecordUpdateRequest request = new TrainingRecordUpdateRequest();
        request.setTrainingStatus("completed");
        assertThrows(BusinessException.class, () -> service.update(1L, request));
        request.setTrainingStatus("failed");
        assertThrows(BusinessException.class, () -> service.update(1L, request));
        request.setTrainingStatus("learning");
        when(recordMapper.updateById(any(TrainingRecordEntity.class))).thenReturn(1);
        service.update(1L, request);
    }

    @Test
    void zeroAffectedAndAuditFailurePropagate() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(recordMapper.selectById(1L)).thenReturn(record());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        TrainingRecordUpdateRequest request = new TrainingRecordUpdateRequest();
        request.setTrainingStatus("learning");
        when(recordMapper.updateById(any(TrainingRecordEntity.class))).thenReturn(0);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request))
                        .getResultCode());

        when(recordMapper.updateById(any(TrainingRecordEntity.class))).thenReturn(1);
        doThrow(new IllegalStateException("audit")).when(auditLogService).record(any());
        assertThrows(IllegalStateException.class, () -> service.update(1L, request));
    }

    private TrainingRecordCreateRequest createRequest() {
        TrainingRecordCreateRequest r = new TrainingRecordCreateRequest();
        r.setPlanId(1L);
        r.setUserId(8L);
        return r;
    }

    private TrainingPlanEntity plan(String status) {
        TrainingPlanEntity p = new TrainingPlanEntity();
        p.setId(1L);
        p.setPublishStatus(status);
        p.setStatus(1);
        p.setIsDeleted(0);
        return p;
    }

    private TrainingRecordEntity record() {
        TrainingRecordEntity r = new TrainingRecordEntity();
        r.setId(1L);
        r.setPlanId(1L);
        r.setUserId(8L);
        r.setTrainingStatus("not_started");
        r.setAttendanceStatus("pending");
        r.setProgress(BigDecimal.ZERO);
        return r;
    }

    private UserEntity user(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setStatus(1);
        u.setIsDeleted(0);
        return u;
    }
}
