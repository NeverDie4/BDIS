package com.bdis.modules.course.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.course.request.CourseLearningProgressRequest;
import com.bdis.modules.course.service.CourseLearningProgressService;
import com.bdis.modules.course.vo.CourseLearningProgressVO;
import com.bdis.modules.course.vo.CourseLearningSummaryVO;
import com.bdis.modules.permission.service.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/courses/{courseId}/learning")
public class CourseLearningProgressController {
    private final CourseLearningProgressService progressService;
    private final AuthorizationService authorizationService;

    public CourseLearningProgressController(CourseLearningProgressService progressService, AuthorizationService authorizationService) {
        this.progressService = progressService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<CourseLearningProgressVO>> list(@PathVariable @Positive Long courseId) {
        authorizationService.requirePermission("edu:course-learning:list");
        return Result.success(progressService.list(courseId));
    }

    @PutMapping
    public Result<CourseLearningProgressVO> save(@PathVariable @Positive Long courseId, @Valid @RequestBody CourseLearningProgressRequest request) {
        authorizationService.requirePermission("edu:course-learning:save");
        return Result.success(progressService.save(courseId, request));
    }

    @GetMapping("/summary")
    public Result<CourseLearningSummaryVO> summary(@PathVariable @Positive Long courseId) {
        authorizationService.requirePermission("edu:course:learning-summary");
        return Result.success(progressService.summary(courseId));
    }
}
