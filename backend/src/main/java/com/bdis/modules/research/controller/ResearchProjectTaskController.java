package com.bdis.modules.research.controller;

import com.bdis.common.core.Result;
import com.bdis.modules.permission.service.AuthorizationService;
import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import com.bdis.modules.research.request.ResearchProjectTaskCreateRequest;
import com.bdis.modules.research.request.ResearchProjectTaskMemberRequest;
import com.bdis.modules.research.entity.ResearchProjectTaskMemberEntity;
import com.bdis.modules.research.service.ResearchProjectTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/research-projects")
public class ResearchProjectTaskController {
    private final ResearchProjectTaskService service; private final AuthorizationService auth;
    public ResearchProjectTaskController(ResearchProjectTaskService service, AuthorizationService auth){this.service=service;this.auth=auth;}
    @PostMapping("/{projectId}/tasks") public Result<Long> create(@PathVariable @Positive Long projectId,@Valid @RequestBody ResearchProjectTaskCreateRequest request){auth.requirePermission("research:project:task");return Result.success(service.create(projectId,request));}
    @GetMapping("/{projectId}/tasks") public Result<List<ResearchProjectTaskEntity>> list(@PathVariable @Positive Long projectId){auth.requirePermission("research:project:submission:list");return Result.success(service.list(projectId));}
    @PostMapping("/tasks/{taskId}/accept") public Result<Void> accept(@PathVariable @Positive Long taskId){auth.requirePermission("research:project:submission:add");service.accept(taskId);return Result.success();}
    @PostMapping("/tasks/{taskId}/members") public Result<Void> assign(@PathVariable @Positive Long taskId,@Valid @RequestBody ResearchProjectTaskMemberRequest request){auth.requirePermission("research:project:task");service.assignMember(taskId,request);return Result.success();}
    @GetMapping("/tasks/{taskId}/members") public Result<List<ResearchProjectTaskMemberEntity>> members(@PathVariable @Positive Long taskId){auth.requirePermission("research:project:submission:list");return Result.success(service.members(taskId));}
}
