package com.bdis.modules.training.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.entity.TrainingRecordItemEntity;
import com.bdis.modules.training.request.TrainingRecordItemProgressRequest;
import com.bdis.modules.training.service.TrainingRecordItemProgressService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-records/{recordId}/items")
public class TrainingRecordItemProgressController {
    private final TrainingRecordItemProgressService service;
    private final AuthorizationService authorizationService;

    public TrainingRecordItemProgressController(
            TrainingRecordItemProgressService service, AuthorizationService authorizationService) {
        this.service = service;
        this.authorizationService = authorizationService;
    }

    @PutMapping("/{itemId}")
    public Result<TrainingRecordItemEntity> save(
            @PathVariable @Positive Long recordId,
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody TrainingRecordItemProgressRequest request) {
        authorizationService.requirePermission("edu:training-record:item-progress");
        return Result.success(service.save(recordId, itemId, request));
    }
}
