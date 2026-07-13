package com.bdis.modules.course.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.course.request.ExperimentStepCreateRequest;
import com.bdis.modules.course.request.ExperimentStepUpdateRequest;
import com.bdis.modules.course.service.ExperimentStepService;
import com.bdis.modules.course.vo.ExperimentStepVO;
import com.bdis.modules.permission.service.AuthorizationService;
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
@RequestMapping("/courses/{courseId}/steps")
public class ExperimentStepController {

    private final ExperimentStepService stepService;
    private final AuthorizationService authorizationService;

    public ExperimentStepController(
            ExperimentStepService stepService, AuthorizationService authorizationService) {
        this.stepService = stepService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<ExperimentStepVO>> list(
            @PathVariable @Positive Long courseId) {
        authorizationService.requirePermission("edu:course-step:list");
        return Result.success(stepService.listByCourseId(courseId));
    }

    @PostMapping
    public Result<ExperimentStepVO> create(
            @PathVariable @Positive Long courseId,
            @Valid @RequestBody ExperimentStepCreateRequest request) {
        authorizationService.requirePermission("edu:course-step:save");
        return Result.success(stepService.create(courseId, request));
    }

    @PutMapping("/{stepId}")
    public Result<ExperimentStepVO> update(
            @PathVariable @Positive Long courseId,
            @PathVariable @Positive Long stepId,
            @Valid @RequestBody ExperimentStepUpdateRequest request) {
        authorizationService.requirePermission("edu:course-step:save");
        return Result.success(stepService.update(courseId, stepId, request));
    }

    @DeleteMapping("/{stepId}")
    public Result<Void> delete(
            @PathVariable @Positive Long courseId, @PathVariable @Positive Long stepId) {
        authorizationService.requirePermission("edu:course-step:delete");
        stepService.delete(courseId, stepId);
        return Result.success();
    }
}
