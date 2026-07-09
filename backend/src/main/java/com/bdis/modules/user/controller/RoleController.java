package com.bdis.modules.user.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.dto.RoleCreateDTO;
import com.bdis.modules.user.dto.RolePermissionAssignDTO;
import com.bdis.modules.user.dto.RoleUpdateDTO;
import com.bdis.modules.user.query.RoleQuery;
import com.bdis.modules.user.service.RoleService;
import com.bdis.modules.user.vo.RoleVO;
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
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    private final AuthorizationService authorizationService;

    public RoleController(RoleService roleService, AuthorizationService authorizationService) {
        this.roleService = roleService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<RoleVO>> page(@Valid RoleQuery query) {
        authorizationService.requirePermission("auth:role:view");
        return Result.success(roleService.page(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody RoleCreateDTO dto) {
        authorizationService.requirePermission("auth:role:create");
        return Result.success(roleService.create(dto));
    }

    @GetMapping("/{roleId}")
    public Result<RoleVO> detail(@PathVariable Long roleId) {
        authorizationService.requirePermission("auth:role:view");
        return Result.success(roleService.detail(roleId));
    }

    @PutMapping("/{roleId}")
    public Result<Void> update(@PathVariable Long roleId, @Valid @RequestBody RoleUpdateDTO dto) {
        authorizationService.requirePermission("auth:role:update");
        roleService.update(roleId, dto);
        return Result.success();
    }

    @DeleteMapping("/{roleId}")
    public Result<Void> delete(@PathVariable Long roleId) {
        authorizationService.requirePermission("auth:role:delete");
        roleService.delete(roleId);
        return Result.success();
    }

    @PutMapping("/{roleId}/permissions")
    public Result<Void> assignPermissions(
            @PathVariable Long roleId, @Valid @RequestBody RolePermissionAssignDTO dto) {
        authorizationService.requirePermission("auth:role:assign-permission");
        roleService.assignPermissions(roleId, dto);
        return Result.success();
    }
}
