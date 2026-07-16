package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.mapper.TrainingRecordMapper;
import com.bdis.modules.training.service.impl.TrainingSummaryServiceImpl;
import com.bdis.modules.training.vo.TrainingSummaryVO;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrainingSummaryServiceTest {
    @Mock TrainingPlanMapper planMapper;
    @Mock TrainingRecordMapper recordMapper;
    @Mock TrainingPlanService planService;
    TrainingSummaryService service;

    @BeforeEach
    void setUp() {
        service = new TrainingSummaryServiceImpl(planMapper, recordMapper, planService);
    }

    @Test
    void returnsSingleAggregateWithAllCountersAndNullableAverages() {
        TrainingPlanEntity plan = plan();
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan);
        TrainingSummaryVO summary = new TrainingSummaryVO();
        summary.setPlanId(1L);
        summary.setTotalParticipantCount(5L);
        summary.setCompletedCount(2L);
        summary.setAbsentCount(1L);
        summary.setAverageProgress(new BigDecimal("60.00"));
        summary.setScoredParticipantCount(2L);
        summary.setAverageScore(new BigDecimal("80.00"));
        summary.setFeedbackCount(3L);
        summary.setAverageRating(new BigDecimal("4.00"));
        when(recordMapper.selectSummary(1L)).thenReturn(summary);

        TrainingSummaryVO result = service.getSummary(1L);

        assertEquals(5L, result.getTotalParticipantCount());
        assertEquals(2L, result.getScoredParticipantCount());
        assertEquals(3L, result.getFeedbackCount());
        verify(recordMapper, times(1)).selectSummary(1L);
    }

    @Test
    void emptyPlanUsesZeroCountsButKeepsNoScoreAndNoRatingAsNull() {
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan());
        TrainingSummaryVO summary = new TrainingSummaryVO();
        summary.setPlanId(1L);
        when(recordMapper.selectSummary(1L)).thenReturn(summary);

        TrainingSummaryVO result = service.getSummary(1L);

        assertEquals(0L, result.getTotalParticipantCount());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getAverageProgress());
        assertNull(result.getAverageScore());
        assertNull(result.getAverageRating());
    }

    @Test
    void missingOrDeletedPlanIsNotFound() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getSummary(1L))
                        .getResultCode());
        TrainingPlanEntity deleted = plan();
        deleted.setIsDeleted(1);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getSummary(1L))
                        .getResultCode());
    }

    @Test
    void rejectsSummaryOutsideTrainingPlanScope() {
        doThrow(new ForbiddenException("Training plan is outside the current user's scope"))
                .when(planService)
                .requireViewAccess(1L);

        assertThrows(ForbiddenException.class, () -> service.getSummary(1L));
        verifyNoInteractions(recordMapper);
    }

    private TrainingPlanEntity plan() {
        TrainingPlanEntity plan = new TrainingPlanEntity();
        plan.setId(1L);
        plan.setPlanNo("P1");
        plan.setPlanName("Plan");
        plan.setPublishStatus("closed");
        plan.setStatus(1);
        plan.setIsDeleted(0);
        return plan;
    }
}
