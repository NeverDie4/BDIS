package com.bdis.modules.research.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.query.ResearchAchievementQuery;
import com.bdis.modules.research.request.ResearchAchievementCreateRequest;
import com.bdis.modules.research.request.ResearchAchievementUpdateRequest;
import com.bdis.modules.research.service.ResearchAchievementService;
import com.bdis.modules.research.vo.ResearchAchievementDetailVO;
import com.bdis.modules.research.vo.ResearchAchievementListVO;
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
@RequestMapping("/research-achievements")
public class ResearchAchievementController {
    private final ResearchAchievementService achievementService;
    private final AuthorizationService authorizationService;

    public ResearchAchievementController(
            ResearchAchievementService achievementService,
            AuthorizationService authorizationService) {
        this.achievementService = achievementService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<PageResult<ResearchAchievementListVO>> page(
            @Valid @ModelAttribute ResearchAchievementQuery query) {
        authorizationService.requirePermission("research:achievement:list");
        return Result.success(achievementService.page(query));
    }

    @GetMapping("/{id}")
    public Result<ResearchAchievementDetailVO> detail(@PathVariable @Positive Long id) {
        authorizationService.requirePermission("research:achievement:detail");
        return Result.success(achievementService.getDetail(id));
    }

    @PostMapping
    public Result<ResearchAchievementDetailVO> create(
            @Valid @RequestBody ResearchAchievementCreateRequest request) {
        authorizationService.requirePermission("research:achievement:add");
        return Result.success(achievementService.getDetail(achievementService.create(request)));
    }

    @PutMapping("/{id}")
    public Result<ResearchAchievementDetailVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody ResearchAchievementUpdateRequest request) {
        authorizationService.requirePermission("research:achievement:update");
        achievementService.update(id, request);
        return Result.success(achievementService.getDetail(id));
    }
}
