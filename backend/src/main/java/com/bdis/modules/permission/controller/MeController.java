package com.bdis.modules.permission.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.vo.MenuVO;
import com.bdis.modules.permission.vo.PermissionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/me")
public class MeController {

    private final AuthorizationService authorizationService;

    public MeController(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/menus")
    public Result<List<MenuVO>> menus() {
        return Result.success(authorizationService.currentMenus());
    }

    @GetMapping("/buttons")
    public Result<List<PermissionVO>> buttons(@RequestParam(required = false) String menuCode) {
        return Result.success(authorizationService.currentButtons(menuCode));
    }
}
