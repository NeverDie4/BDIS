package com.bdis.modules.evaluation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.evaluation.dto.EvaluationConfirmationRequest;
import com.bdis.modules.evaluation.dto.EvaluationScoreRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.entity.EvaluationResultEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.mapper.EvaluationIndicatorMapper;
import com.bdis.modules.evaluation.mapper.EvaluationResultMapper;
import com.bdis.modules.evaluation.mapper.EvaluationScoreRecordMapper;
import com.bdis.modules.evaluation.mapper.EvaluationTaskMapper;
import com.bdis.modules.evaluation.service.impl.EvaluationResultServiceImpl;
import com.bdis.modules.evaluation.service.impl.EvaluationScoreServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationWorkflowServiceTest {

    @Mock private EvaluationResultMapper resultMapper;
    @Mock private EvaluationScoreRecordMapper scoreMapper;
    @Mock private EvaluationIndicatorMapper indicatorMapper;
    @Mock private EvaluationTaskMapper taskMapper;
    @Mock private BusinessAccessService accessService;

    @Test
    void averagesScoresByIndicatorBeforeApplyingWeight() {
        EvaluationTaskEntity task = task(1L, "scoring");
        EvaluationScoreRecordEntity first = score(10L, 1L, 80);
        EvaluationScoreRecordEntity second = score(11L, 1L, 100);
        EvaluationIndicatorEntity indicator = new EvaluationIndicatorEntity();
        indicator.setId(1L);
        indicator.setWeight(new BigDecimal("100"));
        indicator.setMaxScore(new BigDecimal("100"));
        when(scoreMapper.selectById(10L)).thenReturn(first);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(scoreMapper.selectList(any())).thenReturn(List.of(first, second));
        when(indicatorMapper.selectByIds(any())).thenReturn(List.of(indicator));
        when(resultMapper.selectOne(any())).thenReturn(null);
        doAnswer(
                        invocation -> {
                            EvaluationResultEntity entity = invocation.getArgument(0);
                            entity.setId(7L);
                            capturedResult = entity;
                            return 1;
                        })
                .when(resultMapper)
                .insert(any(EvaluationResultEntity.class));
        when(resultMapper.selectById(7L)).thenAnswer(invocation -> capturedResult);
        when(accessService.currentUserId()).thenReturn(3L);

        EvaluationConfirmationRequest request = new EvaluationConfirmationRequest();
        EvaluationResultServiceImpl service =
                new EvaluationResultServiceImpl(
                        resultMapper, scoreMapper, indicatorMapper, taskMapper, accessService);

        EvaluationResultEntity result = service.confirmByScoreRecord(10L, request);

        assertThat(result.getTotalScore()).isEqualByComparingTo("90.00");
        assertThat(result.getResultLevel()).isEqualTo("excellent");
        assertThat(result.getConfirmedBy()).isEqualTo(3L);
        assertThat(task.getTaskStatus()).isEqualTo("confirmed");
    }

    @Test
    void normalizesNonHundredMaxScoresBeforeApplyingWeight() {
        EvaluationScoreRecordEntity first = score(20L, 1L, 10);
        EvaluationScoreRecordEntity second = score(21L, 2L, 20);
        EvaluationIndicatorEntity firstIndicator = indicator(1L, 50, 10);
        EvaluationIndicatorEntity secondIndicator = indicator(2L, 50, 20);
        stubSuccessfulConfirmation(
                first, List.of(first, second), List.of(firstIndicator, secondIndicator));

        EvaluationResultEntity result = service().confirmByScoreRecord(20L, request());

        assertThat(result.getTotalScore()).isEqualByComparingTo("100.00");
        assertThat(result.getResultLevel()).isEqualTo("excellent");
    }

    @Test
    void usesEqualNormalizedWeightsWhenAllWeightsAreZero() {
        EvaluationScoreRecordEntity first = score(30L, 1L, 80);
        EvaluationScoreRecordEntity second = score(31L, 2L, 80);
        EvaluationIndicatorEntity firstIndicator = indicator(1L, 0, 100);
        EvaluationIndicatorEntity secondIndicator = indicator(2L, 0, 100);
        stubSuccessfulConfirmation(
                first, List.of(first, second), List.of(firstIndicator, secondIndicator));

        EvaluationResultEntity result = service().confirmByScoreRecord(30L, request());

        assertThat(result.getTotalScore()).isEqualByComparingTo("80.00");
        assertThat(result.getResultLevel()).isEqualTo("good");
    }

    @Test
    void rejectsWeightedIndicatorsWhenWeightTotalIsNotOneHundred() {
        EvaluationScoreRecordEntity first = score(40L, 1L, 80);
        EvaluationScoreRecordEntity second = score(41L, 2L, 80);
        when(scoreMapper.selectById(40L)).thenReturn(first);
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "scoring"));
        when(scoreMapper.selectList(any())).thenReturn(List.of(first, second));
        when(indicatorMapper.selectByIds(any()))
                .thenReturn(List.of(indicator(1L, 30, 100), indicator(2L, 30, 100)));

        assertThatThrownBy(() -> service().confirmByScoreRecord(40L, request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("参与评分的评价指标权重总和必须为 100");
    }

    @Test
    void rejectsScoreChangesAfterTaskConfirmation() {
        EvaluationTaskEntity task = task(1L, "confirmed");
        when(taskMapper.selectById(1L)).thenReturn(task);
        EvaluationScoreServiceImpl service =
                new EvaluationScoreServiceImpl(
                        scoreMapper, taskMapper, indicatorMapper, accessService);
        EvaluationScoreRequest request = new EvaluationScoreRequest();
        request.setTaskId(1L);
        request.setIndicatorId(1L);
        request.setScore(BigDecimal.TEN);

        assertThatThrownBy(() -> service.saveScore(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("已确认的评价任务不能再修改评分");
    }

    private EvaluationResultEntity capturedResult;

    private void stubSuccessfulConfirmation(
            EvaluationScoreRecordEntity record,
            List<EvaluationScoreRecordEntity> scores,
            List<EvaluationIndicatorEntity> indicators) {
        capturedResult = null;
        when(scoreMapper.selectById(record.getId())).thenReturn(record);
        when(taskMapper.selectById(1L)).thenReturn(task(1L, "scoring"));
        when(scoreMapper.selectList(any())).thenReturn(scores);
        when(indicatorMapper.selectByIds(any())).thenReturn(indicators);
        when(resultMapper.selectOne(any())).thenReturn(null);
        doAnswer(
                        invocation -> {
                            EvaluationResultEntity entity = invocation.getArgument(0);
                            entity.setId(7L);
                            capturedResult = entity;
                            return 1;
                        })
                .when(resultMapper)
                .insert(any(EvaluationResultEntity.class));
        when(resultMapper.selectById(7L)).thenAnswer(invocation -> capturedResult);
        when(accessService.currentUserId()).thenReturn(3L);
    }

    private EvaluationResultServiceImpl service() {
        return new EvaluationResultServiceImpl(
                resultMapper, scoreMapper, indicatorMapper, taskMapper, accessService);
    }

    private EvaluationConfirmationRequest request() {
        return new EvaluationConfirmationRequest();
    }

    private static EvaluationTaskEntity task(Long id, String status) {
        EvaluationTaskEntity task = new EvaluationTaskEntity();
        task.setId(id);
        task.setOwnerId(2L);
        task.setTaskStatus(status);
        return task;
    }

    private static EvaluationScoreRecordEntity score(Long id, Long indicatorId, int value) {
        EvaluationScoreRecordEntity score = new EvaluationScoreRecordEntity();
        score.setId(id);
        score.setTaskId(1L);
        score.setIndicatorId(indicatorId);
        score.setScore(BigDecimal.valueOf(value));
        return score;
    }

    private static EvaluationIndicatorEntity indicator(Long id, int weight, int maxScore) {
        EvaluationIndicatorEntity indicator = new EvaluationIndicatorEntity();
        indicator.setId(id);
        indicator.setWeight(BigDecimal.valueOf(weight));
        indicator.setMaxScore(BigDecimal.valueOf(maxScore));
        return indicator;
    }
}
