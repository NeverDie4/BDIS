package com.bdis.modules.evaluation.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.common.core.Result;
import com.bdis.modules.evaluation.dto.EvaluationStandardRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.query.EvaluationStandardQuery;
import com.bdis.modules.evaluation.service.EvaluationStandardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/evaluation-standards")
public class EvaluationStandardController {

    private final EvaluationStandardService evaluationStandardService;

    @GetMapping
    public Result<IPage<EvaluationIndicatorEntity>> listStandards(EvaluationStandardQuery query) {
        return Result.success(evaluationStandardService.listStandards(query));
    }

    @PostMapping
    public Result<EvaluationIndicatorEntity> createStandard(
            @Valid @RequestBody EvaluationStandardRequest request) {
        return Result.success(evaluationStandardService.createStandard(request));
    }

    @PutMapping("/{standardId}")
    public Result<EvaluationIndicatorEntity> updateStandard(
            @PathVariable Long standardId, @Valid @RequestBody EvaluationStandardRequest request) {
        return Result.success(evaluationStandardService.updateStandard(standardId, request));
    }
}
