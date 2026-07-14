package com.bdis.modules.course.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.request.CourseCreateRequest;
import com.bdis.modules.course.request.CourseStatusChangeRequest;
import com.bdis.modules.course.request.CourseUpdateRequest;
import com.bdis.modules.course.service.CourseService;
import com.bdis.modules.course.vo.CourseDetailVO;
import com.bdis.modules.course.vo.CourseListVO;
import com.bdis.modules.permission.service.AuthorizationService;
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
@RequestMapping("/courses")
public class CourseController {

    private final CourseService courseService;
    private final AuthorizationService authorizationService;

    public CourseController(
            CourseService courseService, AuthorizationService authorizationService) {
        this.courseService = courseService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<CourseListVO>> page(@Valid @ModelAttribute CourseQuery query) {
        authorizationService.requirePermission("edu:course:list");
        return Result.success(courseService.page(query));
    }

    @GetMapping("/{id}")
    public Result<CourseDetailVO> detail(@PathVariable @Positive Long id) {
        authorizationService.requirePermission("edu:course:detail");
        return Result.success(courseService.getDetail(id));
    }

    @PostMapping
    public Result<CourseDetailVO> create(@Valid @RequestBody CourseCreateRequest request) {
        authorizationService.requirePermission("edu:course:add");
        return Result.success(courseService.create(request));
    }

    @PutMapping("/{id}")
    public Result<CourseDetailVO> update(
            @PathVariable @Positive Long id, @Valid @RequestBody CourseUpdateRequest request) {
        authorizationService.requirePermission("edu:course:update");
        return Result.success(courseService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        authorizationService.requirePermission("edu:course:delete");
        courseService.delete(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CourseStatusChangeRequest request) {
        authorizationService.requirePermission("edu:course:publish");
        courseService.publish(id, request.getVersion());
        return Result.success();
    }

    @PostMapping("/{id}/offline")
    public Result<Void> offline(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CourseStatusChangeRequest request) {
        authorizationService.requirePermission("edu:course:publish");
        courseService.offline(id, request.getVersion());
        return Result.success();
    }
}
