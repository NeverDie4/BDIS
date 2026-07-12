package com.bdis.modules.evaluation.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.evaluation.dto.EvaluationConfirmationRequest;
import com.bdis.modules.evaluation.dto.EvaluationScoreRequest;
import com.bdis.modules.evaluation.entity.EvaluationResultEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.service.EvaluationResultService;
import com.bdis.modules.evaluation.service.EvaluationScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/evaluation-records")
public class EvaluationScoreController {

    private final EvaluationScoreService evaluationScoreService;

    private final EvaluationResultService evaluationResultService;

    @PostMapping
    public Result<EvaluationScoreRecordEntity> saveScore(
            @Valid @RequestBody EvaluationScoreRequest request) {
        return Result.success(evaluationScoreService.saveScore(request));
    }

    @PostMapping("/{recordId}/confirmations")
    public Result<EvaluationResultEntity> confirmResult(
            @PathVariable Long recordId, @RequestBody EvaluationConfirmationRequest request) {
        return Result.success(evaluationResultService.confirmByScoreRecord(recordId, request));
    }
}
