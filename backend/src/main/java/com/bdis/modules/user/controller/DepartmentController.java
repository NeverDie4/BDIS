package com.bdis.modules.user.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.dto.DepartmentDTO;
import com.bdis.modules.user.query.DepartmentQuery;
import com.bdis.modules.user.service.DepartmentService;
import com.bdis.modules.user.vo.DepartmentVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    private final AuthorizationService authorizationService;

    public DepartmentController(
            DepartmentService departmentService, AuthorizationService authorizationService) {
        this.departmentService = departmentService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<DepartmentVO>> tree(@Valid DepartmentQuery query) {
        authorizationService.requirePermission("auth:department:view");
        return Result.success(departmentService.tree(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody DepartmentDTO dto) {
        authorizationService.requirePermission("auth:department:create");
        return Result.success(departmentService.create(dto));
    }

    @GetMapping("/{departmentId}")
    public Result<DepartmentVO> detail(@PathVariable Long departmentId) {
        authorizationService.requirePermission("auth:department:view");
        return Result.success(departmentService.detail(departmentId));
    }

    @PutMapping("/{departmentId}")
    public Result<Void> update(
            @PathVariable Long departmentId, @Valid @RequestBody DepartmentDTO dto) {
        authorizationService.requirePermission("auth:department:update");
        departmentService.update(departmentId, dto);
        return Result.success();
    }

    @DeleteMapping("/{departmentId}")
    public Result<Void> delete(@PathVariable Long departmentId) {
        authorizationService.requirePermission("auth:department:delete");
        departmentService.delete(departmentId);
        return Result.success();
    }
}
