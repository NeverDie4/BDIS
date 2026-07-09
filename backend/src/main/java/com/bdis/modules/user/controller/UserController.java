package com.bdis.modules.user.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.dto.RoleAssignDTO;
import com.bdis.modules.user.dto.UserCreateDTO;
import com.bdis.modules.user.dto.UserUpdateDTO;
import com.bdis.modules.user.query.UserQuery;
import com.bdis.modules.user.service.UserService;
import com.bdis.modules.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    private final AuthorizationService authorizationService;

    public UserController(UserService userService, AuthorizationService authorizationService) {
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<UserVO>> page(@Valid UserQuery query) {
        authorizationService.requirePermission("auth:user:view");
        return Result.success(userService.page(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        authorizationService.requirePermission("auth:user:create");
        return Result.success(userService.create(dto));
    }

    @GetMapping("/{userId}")
    public Result<UserVO> detail(@PathVariable Long userId) {
        authorizationService.requirePermission("auth:user:view");
        return Result.success(userService.detail(userId));
    }

    @PutMapping("/{userId}")
    public Result<Void> update(@PathVariable Long userId, @RequestBody UserUpdateDTO dto) {
        authorizationService.requirePermission("auth:user:update");
        userService.update(userId, dto);
        return Result.success();
    }

    @PatchMapping("/{userId}")
    public Result<Void> patch(@PathVariable Long userId, @RequestBody UserUpdateDTO dto) {
        authorizationService.requirePermission("auth:user:update");
        userService.patch(userId, dto);
        return Result.success();
    }

    @DeleteMapping("/{userId}")
    public Result<Void> delete(@PathVariable Long userId) {
        authorizationService.requirePermission("auth:user:delete");
        userService.delete(userId);
        return Result.success();
    }

    @PutMapping("/{userId}/roles")
    public Result<Void> assignRoles(
            @PathVariable Long userId, @Valid @RequestBody RoleAssignDTO dto) {
        authorizationService.requirePermission("auth:user:assign-role");
        userService.assignRoles(userId, dto);
        return Result.success();
    }
}
