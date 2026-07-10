package com.bdis.modules.user.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.user.dto.OrganizationDTO;
import com.bdis.modules.user.query.OrganizationQuery;
import com.bdis.modules.user.service.OrganizationService;
import com.bdis.modules.user.vo.OrganizationVO;
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
@RequestMapping("/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    private final AuthorizationService authorizationService;

    public OrganizationController(
            OrganizationService organizationService, AuthorizationService authorizationService) {
        this.organizationService = organizationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<OrganizationVO>> page(@Valid OrganizationQuery query) {
        authorizationService.requirePermission("auth:organization:view");
        return Result.success(organizationService.page(query));
    }

    @PostMapping
    public Result<Long> create(@Valid @RequestBody OrganizationDTO dto) {
        authorizationService.requirePermission("auth:organization:create");
        return Result.success(organizationService.create(dto));
    }

    @GetMapping("/{organizationId}")
    public Result<OrganizationVO> detail(@PathVariable Long organizationId) {
        authorizationService.requirePermission("auth:organization:view");
        return Result.success(organizationService.detail(organizationId));
    }

    @PutMapping("/{organizationId}")
    public Result<Void> update(
            @PathVariable Long organizationId, @Valid @RequestBody OrganizationDTO dto) {
        authorizationService.requirePermission("auth:organization:update");
        organizationService.update(organizationId, dto);
        return Result.success();
    }

    @DeleteMapping("/{organizationId}")
    public Result<Void> delete(@PathVariable Long organizationId) {
        authorizationService.requirePermission("auth:organization:delete");
        organizationService.delete(organizationId);
        return Result.success();
    }
}
