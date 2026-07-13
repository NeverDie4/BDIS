package com.bdis.modules.training.controller;

import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.request.TrainingParticipantBatchRequest;
import com.bdis.modules.training.service.TrainingRecordService;
import com.bdis.modules.training.vo.TrainingParticipantBatchResultVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-plans/{planId}/participants")
public class TrainingParticipantController {
    private final TrainingRecordService recordService;
    private final AuthorizationService authorizationService;

    public TrainingParticipantController(
            TrainingRecordService recordService,
            AuthorizationService authorizationService) {
        this.recordService = recordService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/batch")
    public Result<TrainingParticipantBatchResultVO> batchCreate(
            @PathVariable @Positive Long planId,
            @Valid @RequestBody TrainingParticipantBatchRequest request) {
        if (planId == null || planId <= 0) {
            throw new BusinessException("Training plan id must be positive");
        }
        authorizationService.requirePermission("edu:training-record:add");
        return Result.success(recordService.batchCreate(planId, request));
    }
}
