package com.bdis.modules.training.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.training.query.TrainingPlanQuery;
import com.bdis.modules.training.request.TrainingPlanCloseRequest;
import com.bdis.modules.training.request.TrainingPlanCreateRequest;
import com.bdis.modules.training.request.TrainingPlanPublishRequest;
import com.bdis.modules.training.request.TrainingPlanUpdateRequest;
import com.bdis.modules.training.service.TrainingPlanService;
import com.bdis.modules.training.vo.TrainingPlanDetailVO;
import com.bdis.modules.training.vo.TrainingPlanListVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training-plans")
public class TrainingPlanController {
    private final TrainingPlanService planService;
    private final AuthorizationService authorizationService;

    public TrainingPlanController(
            TrainingPlanService planService, AuthorizationService authorizationService) {
        this.planService = planService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<TrainingPlanListVO>> page(
            @Valid @ModelAttribute TrainingPlanQuery query) {
        authorizationService.requirePermission("edu:training-plan:list");
        return Result.success(planService.page(query));
    }

    @GetMapping("/{id}")
    public Result<TrainingPlanDetailVO> detail(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-plan:detail");
        return Result.success(planService.getDetail(id));
    }

    @PostMapping
    public Result<TrainingPlanDetailVO> create(
            @Valid @RequestBody TrainingPlanCreateRequest request) {
        authorizationService.requirePermission("edu:training-plan:add");
        Long id = planService.create(request);
        return Result.success(planService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<TrainingPlanDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody TrainingPlanUpdateRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-plan:update");
        planService.update(id, request);
        return Result.success(planService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-plan:delete");
        planService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<TrainingPlanDetailVO> publish(
            @PathVariable @Positive Long id,
            @Valid @RequestBody TrainingPlanPublishRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-plan:publish");
        planService.publish(id, request);
        return Result.success(planService.getDetail(id));
    }

    @PostMapping("/{id}/close")
    public Result<TrainingPlanDetailVO> close(
            @PathVariable @Positive Long id, @Valid @RequestBody TrainingPlanCloseRequest request) {
        requirePositiveId(id);
        authorizationService.requirePermission("edu:training-plan:publish");
        planService.close(id, request);
        return Result.success(planService.getDetail(id));
    }

    private void requirePositiveId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("Training plan id must be positive");
        }
    }
}
