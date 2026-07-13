package com.bdis.modules.evaluation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.common.security.BusinessAccessService;
import com.bdis.common.security.BusinessReferenceAccessService;
import com.bdis.modules.evaluation.dto.EvaluationTaskRequest;
import com.bdis.modules.evaluation.entity.EvaluationResultEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.mapper.EvaluationResultMapper;
import com.bdis.modules.evaluation.mapper.EvaluationScoreRecordMapper;
import com.bdis.modules.evaluation.mapper.EvaluationTaskMapper;
import com.bdis.modules.evaluation.query.EvaluationTaskQuery;
import com.bdis.modules.evaluation.service.EvaluationTaskService;
import com.bdis.modules.evaluation.vo.EvaluationTaskDetailVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EvaluationTaskServiceImpl implements EvaluationTaskService {

    private static final DateTimeFormatter NO_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final EvaluationTaskMapper taskMapper;

    private final EvaluationScoreRecordMapper scoreRecordMapper;

    private final EvaluationResultMapper resultMapper;

    private final BusinessAccessService accessService;
    private final BusinessReferenceAccessService referenceAccessService;

    @Override
    public IPage<EvaluationTaskEntity> listTasks(EvaluationTaskQuery query) {
        EvaluationTaskQuery safeQuery = query == null ? new EvaluationTaskQuery() : query;
        LambdaQueryWrapper<EvaluationTaskEntity> wrapper =
                new LambdaQueryWrapper<EvaluationTaskEntity>()
                        .like(
                                StringUtils.hasText(safeQuery.getKeyword()),
                                EvaluationTaskEntity::getTaskName,
                                safeQuery.getKeyword())
                        .eq(
                                StringUtils.hasText(safeQuery.getTargetType()),
                                EvaluationTaskEntity::getTargetType,
                                safeQuery.getTargetType())
                        .eq(
                                safeQuery.getTargetId() != null,
                                EvaluationTaskEntity::getTargetId,
                                safeQuery.getTargetId())
                        .eq(
                                StringUtils.hasText(safeQuery.getStatus()),
                                EvaluationTaskEntity::getTaskStatus,
                                safeQuery.getStatus())
                        .orderByDesc(EvaluationTaskEntity::getUpdatedAt);
        accessService.requirePermission("evaluation:task:view");
        accessService.applyOwnerScope(wrapper, "eval_task", EvaluationTaskEntity::getOwnerId);
        return taskMapper.selectPage(
                page(safeQuery.getPageNum(), safeQuery.getPageSize()), wrapper);
    }

    @Override
    public EvaluationTaskEntity createTask(EvaluationTaskRequest request) {
        accessService.requirePermission("evaluation:task:create");
        referenceAccessService.validate(request.getTargetType(), request.getTargetId());
        Long operatorId = accessService.currentUserId();
        EvaluationTaskEntity entity = new EvaluationTaskEntity();
        entity.setTaskNo(defaultText(request.getTaskNo(), generateNo("EVAL-TASK")));
        entity.setTaskName(request.getTaskName());
        entity.setTaskType(request.getTaskType());
        entity.setTargetType(request.getTargetType());
        entity.setTargetId(request.getTargetId());
        entity.setOwnerId(operatorId);
        entity.setStartedAt(request.getStartedAt());
        entity.setEndedAt(request.getEndedAt());
        entity.setTaskStatus("draft");
        entity.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        entity.setCreatedBy(operatorId);
        entity.setRemark(request.getRemark());
        taskMapper.insert(entity);
        return taskMapper.selectById(entity.getId());
    }

    @Override
    public EvaluationTaskDetailVO getTaskDetail(Long taskId) {
        EvaluationTaskEntity task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("评价任务不存在");
        }
        accessService.requireResourceAccess(
                "eval_task", taskId, "evaluation:task:view", task.getOwnerId());
        List<EvaluationScoreRecordEntity> scores =
                scoreRecordMapper.selectList(
                        new LambdaQueryWrapper<EvaluationScoreRecordEntity>()
                                .eq(EvaluationScoreRecordEntity::getTaskId, taskId)
                                .orderByDesc(EvaluationScoreRecordEntity::getScoredAt));
        EvaluationResultEntity result =
                resultMapper.selectOne(
                        new LambdaQueryWrapper<EvaluationResultEntity>()
                                .eq(EvaluationResultEntity::getTaskId, taskId));

        EvaluationTaskDetailVO detail = new EvaluationTaskDetailVO();
        detail.setTask(task);
        detail.setScores(scores);
        detail.setResult(result);
        return detail;
    }

    private Page<EvaluationTaskEntity> page(Long pageNum, Long pageSize) {
        return new Page<>(
                pageNum == null || pageNum < 1 ? 1 : pageNum,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String generateNo(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return prefix + "-" + LocalDateTime.now().format(NO_TIME_FORMAT) + "-" + suffix;
    }
}
