package com.bdis.modules.course.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.course.dto.CourseRequest;
import com.bdis.modules.course.dto.CourseResourceRequest;
import com.bdis.modules.course.dto.ExperimentStepRequest;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.service.CourseService;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.course.vo.CourseVO;
import com.bdis.modules.course.vo.ExperimentStepVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/courses")
@RequirePermission("course:view")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public Result<PageResult<CourseVO>> page(@Valid CourseQuery query) {
        return Result.success(courseService.page(query));
    }

    @GetMapping("/{id}")
    public Result<CourseVO> detail(@PathVariable Long id) {
        return Result.success(courseService.detail(id));
    }

    @PostMapping
    @RequirePermission("course:manage")
    public Result<Long> create(@Valid @RequestBody CourseRequest request) {
        return Result.success(courseService.create(request));
    }

    @PutMapping("/{id}")
    @RequirePermission("course:manage")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        courseService.update(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @RequirePermission("course:manage")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return Result.success();
    }

    @PatchMapping("/{id}/publish-status")
    @RequirePermission("course:publish")
    public Result<CourseVO> publish(
            @PathVariable Long id,
            @RequestParam
                    @Pattern(
                            regexp = "draft|published|archived",
                            message = "发布状态仅支持 draft、published、archived")
                    String status) {
        return Result.success(courseService.publish(id, status));
    }

    @PostMapping("/{courseId}/steps")
    @RequirePermission("course:manage")
    public Result<ExperimentStepVO> addStep(
            @PathVariable Long courseId, @Valid @RequestBody ExperimentStepRequest request) {
        return Result.success(courseService.addStep(courseId, request));
    }

    @PutMapping("/{courseId}/steps/{stepId}")
    @RequirePermission("course:manage")
    public Result<ExperimentStepVO> updateStep(
            @PathVariable Long courseId,
            @PathVariable Long stepId,
            @Valid @RequestBody ExperimentStepRequest request) {
        return Result.success(courseService.updateStep(courseId, stepId, request));
    }

    @DeleteMapping("/{courseId}/steps/{stepId}")
    @RequirePermission("course:manage")
    public Result<Void> deleteStep(@PathVariable Long courseId, @PathVariable Long stepId) {
        courseService.deleteStep(courseId, stepId);
        return Result.success();
    }

    @PostMapping("/{courseId}/resources")
    @RequirePermission("course:manage")
    public Result<CourseResourceVO> addResource(
            @PathVariable Long courseId, @Valid @RequestBody CourseResourceRequest request) {
        return Result.success(courseService.addResource(courseId, request));
    }

    @DeleteMapping("/{courseId}/resources/{resourceId}")
    @RequirePermission("course:manage")
    public Result<Void> deleteResource(@PathVariable Long courseId, @PathVariable Long resourceId) {
        courseService.deleteResource(courseId, resourceId);
        return Result.success();
    }
}
