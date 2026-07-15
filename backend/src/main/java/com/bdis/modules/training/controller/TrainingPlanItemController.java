package com.bdis.modules.training.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import com.bdis.modules.training.request.TrainingPlanItemRequest;
import com.bdis.modules.training.service.TrainingPlanItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-plans/{planId}/items")
public class TrainingPlanItemController {
    private final TrainingPlanItemService service;
    private final AuthorizationService auth;

    public TrainingPlanItemController(TrainingPlanItemService service, AuthorizationService auth) {
        this.service = service;
        this.auth = auth;
    }

    @GetMapping
    public Result<List<TrainingPlanItemEntity>> list(@PathVariable @Positive Long planId) {
        auth.requirePermission("edu:training-item:list");
        return Result.success(service.list(planId));
    }

    @PostMapping
    public Result<Long> create(
            @PathVariable @Positive Long planId,
            @Valid @RequestBody TrainingPlanItemRequest request) {
        auth.requirePermission("edu:training-item:save");
        return Result.success(service.create(planId, request));
    }

    @PutMapping("/{itemId}")
    public Result<Void> update(
            @PathVariable @Positive Long itemId,
            @Valid @RequestBody TrainingPlanItemRequest request) {
        auth.requirePermission("edu:training-item:save");
        service.update(itemId, request);
        return Result.success();
    }

    @DeleteMapping("/{itemId}")
    public Result<Void> delete(@PathVariable @Positive Long itemId) {
        auth.requirePermission("edu:training-item:save");
        service.delete(itemId);
        return Result.success();
    }
}
