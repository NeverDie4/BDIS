package com.bdis.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EvaluationResultServiceImpl implements EvaluationResultService {

    private final EvaluationResultMapper resultMapper;

    private final EvaluationScoreRecordMapper scoreRecordMapper;

    private final EvaluationIndicatorMapper indicatorMapper;

    private final EvaluationTaskMapper taskMapper;

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

        BigDecimal totalScore =
                request.getTotalScore() == null
                        ? calculateTotalScore(record.getTaskId())
                        : request.getTotalScore();

        EvaluationResultEntity result =
                resultMapper.selectOne(
                        new LambdaQueryWrapper<EvaluationResultEntity>()
                                .eq(EvaluationResultEntity::getTaskId, record.getTaskId()));
        if (result == null) {
            result = new EvaluationResultEntity();
            result.setTaskId(record.getTaskId());
        }
        result.setTotalScore(totalScore);
        result.setResultLevel(defaultText(request.getResultLevel(), inferLevel(totalScore)));
        result.setResultDesc(request.getResultDesc());
        result.setConfirmedBy(request.getConfirmedBy());
        result.setConfirmedAt(LocalDateTime.now());
        result.setRemark(request.getRemark());

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

        boolean hasWeight =
                indicators.values().stream()
                        .anyMatch(
                                indicator ->
                                        indicator.getWeight() != null
                                                && indicator.getWeight().compareTo(BigDecimal.ZERO)
                                                        > 0);

        BigDecimal total = BigDecimal.ZERO;
        for (EvaluationScoreRecordEntity score : scores) {
            if (score.getScore() == null) {
                continue;
            }
            EvaluationIndicatorEntity indicator = indicators.get(score.getIndicatorId());
            if (hasWeight && indicator != null && indicator.getWeight() != null) {
                total =
                        total.add(
                                score.getScore()
                                        .multiply(indicator.getWeight())
                                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            } else {
                total = total.add(score.getScore());
            }
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

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }
}
