package com.bdis.modules.course.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.course.service.CourseEnrollmentService;
import com.bdis.modules.course.vo.CourseEnrollmentVO;
import com.bdis.modules.permission.service.AuthorizationService;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/courses")
public class CourseEnrollmentController {
    private final CourseEnrollmentService enrollmentService;
    private final AuthorizationService authorizationService;

    public CourseEnrollmentController(
            CourseEnrollmentService enrollmentService, AuthorizationService authorizationService) {
        this.enrollmentService = enrollmentService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/{courseId}/enrollment")
    public Result<CourseEnrollmentVO> enroll(@PathVariable @Positive Long courseId) {
        authorizationService.requirePermission("edu:course:enroll");
        return Result.success(enrollmentService.enroll(courseId));
    }

    @GetMapping("/enrollments")
    public Result<List<CourseEnrollmentVO>> mine() {
        authorizationService.requirePermission("edu:course:enrollment:list");
        return Result.success(enrollmentService.listMine());
    }

    @GetMapping("/{courseId}/enrollments")
    public Result<List<CourseEnrollmentVO>> byCourse(@PathVariable @Positive Long courseId) {
        authorizationService.requirePermission("edu:course:enrollment:list");
        return Result.success(enrollmentService.listByCourse(courseId));
    }
}
