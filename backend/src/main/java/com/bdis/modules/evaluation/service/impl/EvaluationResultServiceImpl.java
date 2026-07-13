package com.bdis.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.evaluation.dto.EvaluationConfirmationRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.entity.EvaluationResultEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.mapper.EvaluationIndicatorMapper;
import com.bdis.modules.evaluation.mapper.EvaluationResultMapper;
import com.bdis.modules.evaluation.mapper.EvaluationScoreRecordMapper;
import com.bdis.modules.evaluation.mapper.EvaluationTaskMapper;
import com.bdis.modules.evaluation.service.EvaluationResultService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationResultServiceImpl implements EvaluationResultService {

    private final EvaluationResultMapper resultMapper;

    private final EvaluationScoreRecordMapper scoreRecordMapper;

    private final EvaluationIndicatorMapper indicatorMapper;

    private final EvaluationTaskMapper taskMapper;

    private final BusinessAccessService accessService;

    @Override
    @Transactional
    public EvaluationResultEntity confirmByScoreRecord(
            Long recordId, EvaluationConfirmationRequest request) {
        EvaluationScoreRecordEntity record = scoreRecordMapper.selectById(recordId);
        if (record == null) {
            throw new IllegalArgumentException("评价评分记录不存在");
        }
        EvaluationTaskEntity task = taskMapper.selectById(record.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("评价任务不存在");
        }
        accessService.requireResourceAccess(
                "eval_task", task.getId(), "evaluation:result:confirm", task.getOwnerId());
        if ("confirmed".equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("评价任务已确认，不能重复确认");
        }

        BigDecimal totalScore = calculateTotalScore(record.getTaskId());

        EvaluationResultEntity result =
                resultMapper.selectOne(
                        new LambdaQueryWrapper<EvaluationResultEntity>()
                                .eq(EvaluationResultEntity::getTaskId, record.getTaskId()));
        if (result == null) {
            result = new EvaluationResultEntity();
            result.setTaskId(record.getTaskId());
        }
        result.setTotalScore(totalScore);
        result.setResultLevel(inferLevel(totalScore));
        result.setResultDesc(request.getResultDesc());
        result.setConfirmedBy(accessService.currentUserId());
        result.setConfirmedAt(LocalDateTime.now());
        result.setRemark(request.getRemark());
        result.setCreatedBy(accessService.currentUserId());

        if (result.getId() == null) {
            resultMapper.insert(result);
        } else {
            resultMapper.updateById(result);
        }

        task.setTaskStatus("confirmed");
        taskMapper.updateById(task);
        return resultMapper.selectById(result.getId());
    }

    private BigDecimal calculateTotalScore(Long taskId) {
        List<EvaluationScoreRecordEntity> scores =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<EvaluationScoreRecordEntity>()
                                .eq(EvaluationScoreRecordEntity::getTaskId, taskId));
        if (scores.isEmpty()) {
            return BigDecimal.ZERO;
        }

        List<Long> indicatorIds =
                scores.stream()
                        .map(EvaluationScoreRecordEntity::getIndicatorId)
                        .distinct()
                        .toList();
        Map<Long, EvaluationIndicatorEntity> indicators =
                indicatorMapper.selectByIds(indicatorIds).stream()
                        .collect(
                                Collectors.toMap(
                                        EvaluationIndicatorEntity::getId, Function.identity()));

        Map<Long, BigDecimal> averageScores =
                scores.stream()
                        .filter(score -> score.getScore() != null)
                        .collect(
                                Collectors.groupingBy(
                                        EvaluationScoreRecordEntity::getIndicatorId,
                                        Collectors.collectingAndThen(
                                                Collectors.mapping(
                                                        EvaluationScoreRecordEntity::getScore,
                                                        Collectors.toList()),
                                                values ->
                                                        values.stream()
                                                                .filter(Objects::nonNull)
                                                                .reduce(
                                                                        BigDecimal.ZERO,
                                                                        BigDecimal::add)
                                                                .divide(
                                                                        BigDecimal.valueOf(
                                                                                values.size()),
                                                                        4,
                                                                        RoundingMode.HALF_UP))));
        if (averageScores.isEmpty()) {
            throw new IllegalArgumentException("评价任务没有有效评分记录");
        }

        List<EvaluationIndicatorEntity> scoredIndicators =
                averageScores.keySet().stream()
                        .map(indicators::get)
                        .filter(Objects::nonNull)
                        .toList();
        if (scoredIndicators.size() != averageScores.size()) {
            throw new IllegalArgumentException("评分记录关联的评价指标不存在");
        }
        for (EvaluationIndicatorEntity indicator : scoredIndicators) {
            if (indicator.getMaxScore() == null
                    || indicator.getMaxScore().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("评价指标满分必须大于 0");
            }
        }

        boolean allWeightsZero =
                scoredIndicators.stream()
                        .allMatch(
                                indicator ->
                                        indicator.getWeight() == null
                                                || indicator.getWeight().compareTo(BigDecimal.ZERO)
                                                        == 0);
        if (!allWeightsZero) {
            boolean hasZeroWeight =
                    scoredIndicators.stream()
                            .anyMatch(
                                    indicator ->
                                            indicator.getWeight() == null
                                                    || indicator
                                                                    .getWeight()
                                                                    .compareTo(BigDecimal.ZERO)
                                                            <= 0);
            if (hasZeroWeight) {
                throw new IllegalArgumentException("评价指标不能混合配置零权重和非零权重");
            }
            BigDecimal totalWeight =
                    scoredIndicators.stream()
                            .map(EvaluationIndicatorEntity::getWeight)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalWeight.compareTo(new BigDecimal("100")) != 0) {
                throw new IllegalArgumentException("参与评分的评价指标权重总和必须为 100");
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : averageScores.entrySet()) {
            EvaluationIndicatorEntity indicator = indicators.get(entry.getKey());
            BigDecimal normalizedScore =
                    entry.getValue()
                            .multiply(new BigDecimal("100"))
                            .divide(indicator.getMaxScore(), 8, RoundingMode.HALF_UP);
            if (allWeightsZero) {
                total = total.add(normalizedScore);
            } else {
                total =
                        total.add(
                                normalizedScore
                                        .multiply(indicator.getWeight())
                                        .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
            }
        }
        if (allWeightsZero) {
            total = total.divide(BigDecimal.valueOf(averageScores.size()), 8, RoundingMode.HALF_UP);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String inferLevel(BigDecimal totalScore) {
        if (totalScore.compareTo(new BigDecimal("90")) >= 0) {
            return "excellent";
        }
        if (totalScore.compareTo(new BigDecimal("80")) >= 0) {
            return "good";
        }
        if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
            return "qualified";
        }
        return "unqualified";
    }
}
