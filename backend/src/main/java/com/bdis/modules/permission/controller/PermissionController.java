package com.bdis.modules.permission.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.dto.PermissionDTO;
import com.bdis.modules.permission.query.PermissionQuery;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.PermissionService;
import com.bdis.modules.permission.vo.PermissionVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    private final AuthorizationService authorizationService;

    public PermissionController(
            PermissionService permissionService, AuthorizationService authorizationService) {
        this.permissionService = permissionService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<PermissionVO>> page(@Valid PermissionQuery query) {
        authorizationService.requirePermission("auth:permission:view");
        return Result.success(permissionService.page(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody PermissionDTO dto) {
        authorizationService.requirePermission("auth:permission:create");
        return Result.success(permissionService.create(dto));
    }

    @GetMapping("/{permissionId}")
    public Result<PermissionVO> detail(@PathVariable Long permissionId) {
        authorizationService.requirePermission("auth:permission:view");
        return Result.success(permissionService.detail(permissionId));
    }

    @PutMapping("/{permissionId}")
    public Result<Void> update(
            @PathVariable Long permissionId, @Valid @RequestBody PermissionDTO dto) {
        authorizationService.requirePermission("auth:permission:update");
        permissionService.update(permissionId, dto);
        return Result.success();
    }

    @DeleteMapping("/{permissionId}")
    public Result<Void> delete(@PathVariable Long permissionId) {
        authorizationService.requirePermission("auth:permission:delete");
        permissionService.delete(permissionId);
        return Result.success();
    }
}
