package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.training.entity.TrainingFeedbackEntity;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.mapper.TrainingFeedbackMapper;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.query.TrainingFeedbackQuery;
import com.bdis.modules.training.request.TrainingFeedbackCreateRequest;
import com.bdis.modules.training.request.TrainingFeedbackUpdateRequest;
import com.bdis.modules.training.service.impl.TrainingFeedbackServiceImpl;
import com.bdis.modules.training.vo.TrainingFeedbackListVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
class TrainingFeedbackServiceTest {
    @Mock TrainingFeedbackMapper feedbackMapper;
    @Mock TrainingRecordMapper recordMapper;
    @Mock TrainingPlanMapper planMapper;
    @Mock AuditLogService auditLogService;
    TrainingFeedbackService service;

    @BeforeEach
    void setUp() {
        service =
                new TrainingFeedbackServiceImpl(
                        feedbackMapper, recordMapper, planMapper, auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("8", "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageUsesJoinedMapperQueryAndValidatesFiltersAndRange() {
        TrainingFeedbackListVO vo = new TrainingFeedbackListVO();
        vo.setId(1L);
        vo.setPlanName("Plan");
        when(feedbackMapper.selectPageVO(any(), any()))
                .thenAnswer(
                        invocation -> {
                            Page<TrainingFeedbackListVO> page = invocation.getArgument(0);
                            page.setRecords(List.of(vo));
                            page.setTotal(1);
                            return page;
                        });
        TrainingFeedbackQuery query = new TrainingFeedbackQuery();
        query.setPlanId(1L);
        query.setUserId(8L);
        query.setRating(new BigDecimal("5"));
        query.setSortField("submittedAt");
        query.setSortOrder("desc");
        assertEquals(1, service.page(query).getTotal());
        verify(feedbackMapper).selectPageVO(any(), same(query));

        query.setRating(new BigDecimal("6"));
        assertThrows(BusinessException.class, () -> service.page(query));
        query.setRating(BigDecimal.ONE);
        query.setSubmittedFrom(LocalDateTime.of(2026, 7, 2, 0, 0));
        query.setSubmittedTo(LocalDateTime.of(2026, 7, 1, 0, 0));
        assertThrows(BusinessException.class, () -> service.page(query));
        query.setSubmittedFrom(null);
        query.setSubmittedTo(null);
        query.setSortField("unsafe");
        assertThrows(BusinessException.class, () -> service.page(query));
    }

    @Test
    void createUsesCurrentParticipantAndAllowsPublishedOrClosed() {
        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(feedbackMapper.insert(any(TrainingFeedbackEntity.class)))
                .thenAnswer(
                        invocation -> {
                            TrainingFeedbackEntity entity = invocation.getArgument(0);
                            entity.setId(21L);
                            return 1;
                        });

        assertEquals(21L, service.create(createRequest(new BigDecimal("1"))));
        ArgumentCaptor<TrainingFeedbackEntity> captor =
                ArgumentCaptor.forClass(TrainingFeedbackEntity.class);
        verify(feedbackMapper).insert(captor.capture());
        assertEquals(8L, captor.getValue().getUserId());
        assertNotNull(captor.getValue().getSubmittedAt());
        verify(auditLogService).record(any());

        reset(feedbackMapper);
        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        when(feedbackMapper.insert(any(TrainingFeedbackEntity.class))).thenReturn(1);
        service.create(createRequest(new BigDecimal("5")));
    }

    @Test
    void createRejectsDraftWrongOwnerInvalidRatingAndDuplicate() {
        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("draft"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.create(createRequest(BigDecimal.ONE)))
                        .getResultCode());

        when(recordMapper.selectById(10L)).thenReturn(record(9L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        assertEquals(
                ResultCodeEnum.FORBIDDEN,
                assertThrows(
                                BusinessException.class,
                                () -> service.create(createRequest(BigDecimal.ONE)))
                        .getResultCode());

        assertThrows(BusinessException.class, () -> service.create(createRequest(BigDecimal.ZERO)));
        assertThrows(
                BusinessException.class, () -> service.create(createRequest(new BigDecimal("6"))));

        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(feedbackMapper.selectByRecordAndUser(10L, 8L)).thenReturn(feedback(8L));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.create(createRequest(BigDecimal.ONE)))
                        .getResultCode());
    }

    @Test
    void databaseUniqueConflictBecomesBusinessConflict() {
        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("published"));
        when(feedbackMapper.insert(any(TrainingFeedbackEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(
                                BusinessException.class,
                                () -> service.create(createRequest(BigDecimal.ONE)))
                        .getResultCode());
    }

    @Test
    void updateOwnFeedbackPreservesOwnershipAndSubmittedTimeAndAudits() {
        TrainingFeedbackEntity entity = feedback(8L);
        LocalDateTime submittedAt = entity.getSubmittedAt();
        when(feedbackMapper.selectById(3L)).thenReturn(entity);
        when(recordMapper.selectById(10L)).thenReturn(record(8L));
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan("closed"));
        when(feedbackMapper.updateById(any(TrainingFeedbackEntity.class))).thenReturn(1);
        TrainingFeedbackUpdateRequest request = new TrainingFeedbackUpdateRequest();
        request.setRating(new BigDecimal("4"));
        request.setFeedbackContent("updated");

        service.update(3L, request);

        assertEquals(10L, entity.getTrainingRecordId());
        assertEquals(8L, entity.getUserId());
        assertEquals(submittedAt, entity.getSubmittedAt());
        verify(auditLogService).record(any());
    }

    @Test
    void updateRejectsOtherUsersAndMissingFeedbackWithoutFakeVersion() {
        TrainingFeedbackEntity entity = feedback(9L);
        when(feedbackMapper.selectById(3L)).thenReturn(entity);
        assertEquals(
                ResultCodeEnum.FORBIDDEN,
                assertThrows(BusinessException.class, () -> service.update(3L, updateRequest()))
                        .getResultCode());
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.update(4L, updateRequest()))
                        .getResultCode());
        assertFalse(hasField(TrainingFeedbackUpdateRequest.class, "version"));
    }

    private TrainingFeedbackCreateRequest createRequest(BigDecimal rating) {
        TrainingFeedbackCreateRequest request = new TrainingFeedbackCreateRequest();
        request.setTrainingRecordId(10L);
        request.setRating(rating);
        request.setFeedbackContent("good");
        return request;
    }

    private TrainingFeedbackUpdateRequest updateRequest() {
        TrainingFeedbackUpdateRequest request = new TrainingFeedbackUpdateRequest();
        request.setRating(new BigDecimal("4"));
        return request;
    }

    private TrainingRecordEntity record(Long userId) {
        TrainingRecordEntity record = new TrainingRecordEntity();
        record.setId(10L);
        record.setPlanId(1L);
        record.setUserId(userId);
        return record;
    }

    private TrainingPlanEntity plan(String status) {
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(1L);
        plan.setStatus(1);
        plan.setIsDeleted(0);
        plan.setPublishStatus(status);
        return plan;
    }

    private TrainingFeedbackEntity feedback(Long userId) {
        TrainingFeedbackEntity feedback = new TrainingFeedbackEntity();
        feedback.setId(3L);
        feedback.setTrainingRecordId(10L);
        feedback.setUserId(userId);
        feedback.setSubmittedAt(LocalDateTime.of(2026, 7, 1, 12, 0));
        return feedback;
    }

    private boolean hasField(Class<?> type, String name) {
        try {
            type.getDeclaredField(name);
            return true;
        } catch (NoSuchFieldException exception) {
            return false;
        }
    }
}
