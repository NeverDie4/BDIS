package com.bdis.modules.research.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.request.ProjectMaterialBindRequest;
import com.bdis.modules.research.service.ProjectMaterialService;
import com.bdis.modules.research.vo.ProjectMaterialVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/research-projects/{projectId}/materials")
public class ProjectMaterialController {
    private final ProjectMaterialService materialService;
    private final AuthorizationService authorizationService;

    public ProjectMaterialController(
            ProjectMaterialService materialService, AuthorizationService authorizationService) {
        this.materialService = materialService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<ProjectMaterialVO>> list(
            @PathVariable @Positive Long projectId,
            @RequestParam(required = false) String fileUsage) {
        authorizationService.requirePermission("research:project-material:list");
        return Result.success(materialService.list(projectId, fileUsage));
    }

    @PostMapping
    public Result<Long> bind(
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody ProjectMaterialBindRequest request) {
        authorizationService.requirePermission("research:project-material:add");
        return Result.success(materialService.bind(projectId, request));
    }

    @DeleteMapping("/{fileId}")
    public Result<Void> unbind(
            @PathVariable @Positive Long projectId, @PathVariable @Positive Long fileId) {
        authorizationService.requirePermission("research:project-material:delete");
        materialService.unbind(projectId, fileId);
        return Result.success();
    }
}
