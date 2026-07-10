package com.bdis.modules.permission.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.dto.MenuDTO;
import com.bdis.modules.permission.query.MenuQuery;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.MenuService;
import com.bdis.modules.permission.vo.MenuVO;
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
@RequestMapping("/menus")
public class MenuController {

    private final MenuService menuService;

    private final AuthorizationService authorizationService;

    public MenuController(MenuService menuService, AuthorizationService authorizationService) {
        this.menuService = menuService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<MenuVO>> tree(@Valid MenuQuery query) {
        authorizationService.requirePermission("auth:menu:view");
        return Result.success(menuService.tree(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody MenuDTO dto) {
        authorizationService.requirePermission("auth:menu:create");
        return Result.success(menuService.create(dto));
    }

    @GetMapping("/{menuId}")
    public Result<MenuVO> detail(@PathVariable Long menuId) {
        authorizationService.requirePermission("auth:menu:view");
        return Result.success(menuService.detail(menuId));
    }

    @PutMapping("/{menuId}")
    public Result<Void> update(@PathVariable Long menuId, @Valid @RequestBody MenuDTO dto) {
        authorizationService.requirePermission("auth:menu:update");
        menuService.update(menuId, dto);
        return Result.success();
    }

    @DeleteMapping("/{menuId}")
    public Result<Void> delete(@PathVariable Long menuId) {
        authorizationService.requirePermission("auth:menu:delete");
        menuService.delete(menuId);
        return Result.success();
    }
}
