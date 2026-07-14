package com.bdis.modules.course.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.course.query.CourseResourceQuery;
import com.bdis.modules.course.request.CourseResourceBindRequest;
import com.bdis.modules.course.service.CourseResourceService;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.permission.service.AuthorizationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/courses/{courseId}/resources")
public class CourseResourceController {

    private final CourseResourceService resourceService;
    private final AuthorizationService authorizationService;

    public CourseResourceController(
            CourseResourceService resourceService, AuthorizationService authorizationService) {
        this.resourceService = resourceService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<CourseResourceVO>> list(
            @PathVariable @Positive Long courseId,
            @Valid @ModelAttribute CourseResourceQuery query) {
        authorizationService.requirePermission("edu:course-resource:list");
        return Result.success(resourceService.listByCourseId(courseId, query));
    }

    @PostMapping
    public Result<CourseResourceVO> bind(
            @PathVariable @Positive Long courseId,
            @Valid @RequestBody CourseResourceBindRequest request) {
        authorizationService.requirePermission("edu:course-resource:add");
        return Result.success(resourceService.bind(courseId, request));
    }

    @DeleteMapping("/{resourceId}")
    public Result<Void> unbind(
            @PathVariable @Positive Long courseId, @PathVariable @Positive Long resourceId) {
        authorizationService.requirePermission("edu:course-resource:delete");
        resourceService.unbind(courseId, resourceId);
        return Result.success();
    }
}
