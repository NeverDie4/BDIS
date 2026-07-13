package com.bdis.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.modules.evaluation.dto.EvaluationScoreRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.mapper.EvaluationIndicatorMapper;
import com.bdis.modules.evaluation.mapper.EvaluationScoreRecordMapper;
import com.bdis.modules.evaluation.mapper.EvaluationTaskMapper;
import com.bdis.modules.evaluation.service.EvaluationScoreService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EvaluationScoreServiceImpl implements EvaluationScoreService {

    private final EvaluationScoreRecordMapper scoreRecordMapper;

    private final EvaluationTaskMapper taskMapper;

    private final EvaluationIndicatorMapper indicatorMapper;

    private final BusinessAccessService accessService;

    @Override
    @Transactional
    public EvaluationScoreRecordEntity saveScore(EvaluationScoreRequest request) {
        EvaluationTaskEntity task = taskMapper.selectById(request.getTaskId());
        if (task == null) {
            throw new IllegalArgumentException("评价任务不存在");
        }
        accessService.requireResourceAccess(
                "eval_task", task.getId(), "evaluation:score:create", task.getOwnerId());
        if ("confirmed".equals(task.getTaskStatus())) {
            throw new IllegalArgumentException("已确认的评价任务不能再修改评分");
        }
        EvaluationIndicatorEntity indicator = indicatorMapper.selectById(request.getIndicatorId());
        if (indicator == null) {
            throw new IllegalArgumentException("评价指标不存在");
        }
        if (indicator.getMaxScore() != null
                && request.getScore().compareTo(indicator.getMaxScore()) > 0) {
            throw new IllegalArgumentException("评分不能超过指标满分");
        }

        Long evaluatorId = accessService.currentUserId();
        EvaluationScoreRecordEntity entity =
                scoreRecordMapper.selectOne(
                        new LambdaQueryWrapper<EvaluationScoreRecordEntity>()
                                .eq(EvaluationScoreRecordEntity::getTaskId, request.getTaskId())
                                .eq(
                                        EvaluationScoreRecordEntity::getIndicatorId,
                                        request.getIndicatorId())
                                .eq(EvaluationScoreRecordEntity::getEvaluatorId, evaluatorId));
        if (entity == null) {
            entity = new EvaluationScoreRecordEntity();
            entity.setTaskId(request.getTaskId());
            entity.setIndicatorId(request.getIndicatorId());
            entity.setEvaluatorId(evaluatorId);
            entity.setCreatedBy(evaluatorId);
        }

        entity.setScore(request.getScore());
        entity.setScoreComment(request.getScoreComment());
        entity.setScoredAt(LocalDateTime.now());
        entity.setRemark(request.getRemark());
        entity.setUpdatedBy(evaluatorId);

        if (entity.getId() == null) {
            scoreRecordMapper.insert(entity);
        } else {
            scoreRecordMapper.updateById(entity);
        }

        if (!"confirmed".equals(task.getTaskStatus())) {
            task.setTaskStatus("scoring");
            taskMapper.updateById(task);
        }
        return scoreRecordMapper.selectById(entity.getId());
    }
}
