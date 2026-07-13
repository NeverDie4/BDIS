package com.bdis.modules.training.controller;

import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.request.TrainingPlanMaterialBindRequest;
import com.bdis.modules.training.service.TrainingPlanMaterialService;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/training-plans/{planId}/materials")
public class TrainingPlanMaterialController {
    private final TrainingPlanMaterialService materialService;
    private final AuthorizationService authorizationService;

    public TrainingPlanMaterialController(
            TrainingPlanMaterialService materialService, AuthorizationService authorizationService) {
        this.materialService = materialService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<TrainingPlanMaterialVO>> list(@PathVariable @Positive Long planId) {
        requirePositive(planId, "Training plan id");
        authorizationService.requirePermission("edu:training-material:list");
        return Result.success(materialService.list(planId));
    }

    @PostMapping
    public Result<Long> bind(
            @PathVariable @Positive Long planId,
            @Valid @RequestBody TrainingPlanMaterialBindRequest request) {
        requirePositive(planId, "Training plan id");
        authorizationService.requirePermission("edu:training-material:bind");
        return Result.success(materialService.bind(planId, request));
    }

    @DeleteMapping("/{materialId}")
    public Result<Void> unbind(
            @PathVariable @Positive Long planId,
            @PathVariable @Positive Long materialId) {
        requirePositive(planId, "Training plan id");
        requirePositive(materialId, "Training material id");
        authorizationService.requirePermission("edu:training-material:bind");
        materialService.unbind(planId, materialId);
        return Result.success();
    }

    private void requirePositive(Long id, String label) {
        if (id == null || id <= 0) throw new BusinessException(label + " must be positive");
    }
}
