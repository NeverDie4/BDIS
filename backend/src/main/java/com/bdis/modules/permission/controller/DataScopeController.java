package com.bdis.modules.permission.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.dto.DataScopeDTO;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.permission.vo.DataScopeVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataScopeController {

    private final DataScopeService dataScopeService;

    private final AuthorizationService authorizationService;

    public DataScopeController(
            DataScopeService dataScopeService, AuthorizationService authorizationService) {
        this.dataScopeService = dataScopeService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/roles/{roleId}/data-scopes")
    public Result<List<DataScopeVO>> listByRole(@PathVariable Long roleId) {
        authorizationService.requirePermission("auth:data-scope:view");
        return Result.success(dataScopeService.listByRole(roleId));
    }

    @PostMapping("/data-scopes")
    public Result<Long> save(@Valid @RequestBody DataScopeDTO dto) {
        authorizationService.requirePermission("auth:data-scope:update");
        return Result.success(dataScopeService.save(dto));
    }

    @DeleteMapping("/data-scopes/{dataScopeId}")
    public Result<Void> delete(@PathVariable Long dataScopeId) {
        authorizationService.requirePermission("auth:data-scope:update");
        dataScopeService.delete(dataScopeId);
        return Result.success();
    }

    @GetMapping("/data-scopes/current")
    public Result<DataScopeResultVO> current(@RequestParam String resourceType) {
        return Result.success(dataScopeService.resolveForCurrentUser(resourceType));
    }
}
