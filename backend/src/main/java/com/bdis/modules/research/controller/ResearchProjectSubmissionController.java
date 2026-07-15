package com.bdis.modules.research.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import com.bdis.modules.research.mapper.ResearchProjectSubmissionMapper;
import com.bdis.modules.research.request.ResearchProjectSubmissionCreateRequest;
import com.bdis.modules.research.request.ResearchProjectSubmissionReviewRequest;
import com.bdis.modules.research.service.ResearchProjectSubmissionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/research-projects")
public class ResearchProjectSubmissionController {
    private final ResearchProjectSubmissionService submissionService;
    private final ResearchProjectSubmissionMapper submissionMapper;
    private final AuthorizationService authorizationService;

    public ResearchProjectSubmissionController(ResearchProjectSubmissionService submissionService, ResearchProjectSubmissionMapper submissionMapper, AuthorizationService authorizationService) {
        this.submissionService = submissionService; this.submissionMapper = submissionMapper; this.authorizationService = authorizationService;
    }

    @PostMapping("/{projectId}/submissions")
    public Result<Long> submit(@PathVariable @Positive Long projectId, @Valid @RequestBody ResearchProjectSubmissionCreateRequest request) {
        authorizationService.requirePermission("research:project:submission:add");
        return Result.success(submissionService.submit(projectId, request));
    }

    @GetMapping("/{projectId}/submissions")
    public Result<List<ResearchProjectSubmissionEntity>> list(@PathVariable @Positive Long projectId) {
        authorizationService.requirePermission("research:project:submission:list");
        return Result.success(submissionMapper.selectList(new LambdaQueryWrapper<ResearchProjectSubmissionEntity>().eq(ResearchProjectSubmissionEntity::getProjectId, projectId).orderByDesc(ResearchProjectSubmissionEntity::getSubmittedAt)));
    }

    @PostMapping("/submissions/{submissionId}/review")
    public Result<Void> review(@PathVariable @Positive Long submissionId, @Valid @RequestBody ResearchProjectSubmissionReviewRequest request) {
        authorizationService.requirePermission("research:project:submission:review");
        submissionService.review(submissionId, request); return Result.success();
    }
}
