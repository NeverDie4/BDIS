package com.bdis.modules.research.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.query.ResearchProjectQuery;
import com.bdis.modules.research.request.ResearchProjectCreateRequest;
import com.bdis.modules.research.request.ResearchProjectLeaderChangeRequest;
import com.bdis.modules.research.request.ResearchProjectStatusChangeRequest;
import com.bdis.modules.research.request.ResearchProjectUpdateRequest;
import com.bdis.modules.research.service.ResearchProjectService;
import com.bdis.modules.research.vo.ResearchProjectDetailVO;
import com.bdis.modules.research.vo.ResearchProjectListVO;
import com.bdis.modules.research.vo.ResearchUserCandidateVO;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/research-projects")
public class ResearchProjectController {
    private final ResearchProjectService projectService;
    private final AuthorizationService authorizationService;

    public ResearchProjectController(
            ResearchProjectService projectService, AuthorizationService authorizationService) {
        this.projectService = projectService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<ResearchProjectListVO>> page(
            @Valid @ModelAttribute ResearchProjectQuery query) {
        authorizationService.requirePermission("research:project:list");
        return Result.success(projectService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ResearchProjectDetailVO> detail(@PathVariable @Positive Long id) {
        authorizationService.requirePermission("research:project:detail");
        return Result.success(projectService.getDetail(id));
    }

    @GetMapping("/candidate-users")
    public Result<List<ResearchUserCandidateVO>> candidateUsers() {
        authorizationService.requirePermission("research:project:detail");
        return Result.success(projectService.listUserCandidates());
    }

    @PostMapping
    public Result<ResearchProjectDetailVO> create(
            @Valid @RequestBody ResearchProjectCreateRequest request) {
        authorizationService.requirePermission("research:project:add");
        Long id = projectService.create(request);
        return Result.success(projectService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<ResearchProjectDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ResearchProjectUpdateRequest request) {
        authorizationService.requirePermission("research:project:update");
        projectService.update(id, request);
        return Result.success(projectService.getDetail(id));
    }

    @PostMapping("/{id}/leader")
    public Result<ResearchProjectDetailVO> changeLeader(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ResearchProjectLeaderChangeRequest request) {
        authorizationService.requirePermission("research:project:update");
        projectService.changeLeader(id, request);
        return Result.success(projectService.getDetail(id));
    }

    @PostMapping("/{id}/status")
    public Result<ResearchProjectDetailVO> changeStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ResearchProjectStatusChangeRequest request) {
        authorizationService.requirePermission("research:project:status");
        projectService.changeStatus(id, request);
        return Result.success(projectService.getDetail(id));
    }
}
