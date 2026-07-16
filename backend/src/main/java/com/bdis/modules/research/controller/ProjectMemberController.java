package com.bdis.modules.research.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.request.ProjectInvitationResponseRequest;
import com.bdis.modules.research.request.ProjectMemberAddRequest;
import com.bdis.modules.research.request.ProjectMemberUpdateRequest;
import com.bdis.modules.research.service.ProjectMemberService;
import com.bdis.modules.research.vo.ProjectMemberVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/research-projects/{projectId}/members")
public class ProjectMemberController {
    private final ProjectMemberService memberService;
    private final AuthorizationService authorizationService;

    public ProjectMemberController(
            ProjectMemberService memberService, AuthorizationService authorizationService) {
        this.memberService = memberService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public Result<List<ProjectMemberVO>> list(
            @PathVariable @Positive Long projectId,
            @RequestParam(required = false) String memberStatus) {
        authorizationService.requirePermission("research:project-member:list");
        return Result.success(memberService.list(projectId, memberStatus));
    }

    @PostMapping
    public Result<ProjectMemberVO> add(
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody ProjectMemberAddRequest request) {
        authorizationService.requirePermission("research:project-member:add");
        Long userId = request.getUserId();
        memberService.add(projectId, request);
        return Result.success(memberService.get(projectId, userId));
    }

    @PutMapping("/{userId}")
    public Result<ProjectMemberVO> updateRole(
            @PathVariable @Positive Long projectId,
            @PathVariable @Positive Long userId,
            @Valid @RequestBody ProjectMemberUpdateRequest request) {
        authorizationService.requirePermission("research:project-member:update");
        memberService.updateRole(projectId, userId, request);
        return Result.success(memberService.get(projectId, userId));
    }

    @DeleteMapping("/{userId}")
    public Result<Void> remove(
            @PathVariable @Positive Long projectId, @PathVariable @Positive Long userId) {
        authorizationService.requirePermission("research:project-member:remove");
        memberService.remove(projectId, userId);
        return Result.success();
    }

    @PostMapping("/invite")
    public Result<ProjectMemberVO> invite(
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody ProjectMemberAddRequest request) {
        authorizationService.requirePermission("research:project:invitation");
        memberService.invite(projectId, request);
        return Result.success(memberService.get(projectId, request.getUserId()));
    }

    @PostMapping("/respond")
    public Result<Void> respond(
            @PathVariable @Positive Long projectId,
            @Valid @RequestBody ProjectInvitationResponseRequest request) {
        authorizationService.requirePermission("research:project:invitation:respond");
        memberService.respond(projectId, request.getResponse());
        return Result.success();
    }
}
