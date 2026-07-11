package com.bdis.modules.evaluation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.common.core.Result;
import com.bdis.modules.evaluation.dto.EvaluationTaskRequest;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.query.EvaluationTaskQuery;
import com.bdis.modules.evaluation.service.EvaluationTaskService;
import com.bdis.modules.evaluation.vo.EvaluationTaskDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/evaluation-tasks")
public class EvaluationTaskController {

    private final EvaluationTaskService evaluationTaskService;

    @GetMapping
    public Result<IPage<EvaluationTaskEntity>> listTasks(EvaluationTaskQuery query) {
        return Result.success(evaluationTaskService.listTasks(query));
    }

    @PostMapping
    public Result<EvaluationTaskEntity> createTask(@Valid @RequestBody EvaluationTaskRequest request) {
        return Result.success(evaluationTaskService.createTask(request));
    }

    @GetMapping("/{taskId}")
    public Result<EvaluationTaskDetailVO> getTaskDetail(@PathVariable Long taskId) {
        return Result.success(evaluationTaskService.getTaskDetail(taskId));
    }
}
