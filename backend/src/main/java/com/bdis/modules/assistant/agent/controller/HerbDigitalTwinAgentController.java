package com.bdis.modules.assistant.agent.controller;

import com.bdis.common.core.PageResult;
import com.bdis.common.core.Result;
import com.bdis.common.security.RequirePermission;
import com.bdis.modules.assistant.agent.dto.AgentActionConfirmRequest;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.dto.AgentCollectionPlanUpdateRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskCancelRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskCreateRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskQueryRequest;
import com.bdis.modules.assistant.agent.service.AgentActionExecutionDispatcher;
import com.bdis.modules.assistant.agent.service.AgentCollectionPlanService;
import com.bdis.modules.assistant.agent.service.AgentDigitalArchiveService;
import com.bdis.modules.assistant.agent.service.AgentReanalysisQueryService;
import com.bdis.modules.assistant.agent.service.AgentWaitStatusQueryService;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentWorkflowRunner;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinEvidenceAnalysisRunner;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import com.bdis.modules.assistant.agent.vo.AgentActionVO;
import com.bdis.modules.assistant.agent.vo.AgentAnalysisRoundVO;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveResultVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveStatusVO;
import com.bdis.modules.assistant.agent.vo.AgentDiagnosisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.AgentReanalysisComparisonVO;
import com.bdis.modules.assistant.agent.vo.AgentStepVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.assistant.agent.vo.AgentWaitStatusVO;
import com.bdis.modules.assistant.agent.vo.AgentWorkflowStartVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/herb/assistant/agent")
@RequirePermission("growth:record:view")
public class HerbDigitalTwinAgentController {

  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final HerbDigitalTwinAgentWorkflowRunner workflowRunner;
  private final HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;
  private final AgentCollectionPlanService collectionPlanService;
  private final AgentActionExecutionDispatcher actionExecutionDispatcher;
  private final AgentWaitStatusQueryService waitStatusQueryService;
  private final AgentReanalysisQueryService reanalysisQueryService;
  private final AgentDigitalArchiveService digitalArchiveService;

  public HerbDigitalTwinAgentController(
      HerbDigitalTwinAgentTaskService agentTaskService,
      HerbDigitalTwinAgentWorkflowRunner workflowRunner,
      HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner,
      AgentCollectionPlanService collectionPlanService,
      AgentActionExecutionDispatcher actionExecutionDispatcher,
      AgentWaitStatusQueryService waitStatusQueryService,
      AgentReanalysisQueryService reanalysisQueryService,
      AgentDigitalArchiveService digitalArchiveService) {
    this.agentTaskService = agentTaskService;
    this.workflowRunner = workflowRunner;
    this.evidenceAnalysisRunner = evidenceAnalysisRunner;
    this.collectionPlanService = collectionPlanService;
    this.actionExecutionDispatcher = actionExecutionDispatcher;
    this.waitStatusQueryService = waitStatusQueryService;
    this.reanalysisQueryService = reanalysisQueryService;
    this.digitalArchiveService = digitalArchiveService;
  }

  @PostMapping("/tasks")
  @RequirePermission("growth:record:create")
  public Result<AgentTaskSummaryVO> create(@Valid @RequestBody AgentTaskCreateRequest request) {
    return Result.success(agentTaskService.create(request));
  }

  @GetMapping("/tasks/{agentTaskId}")
  public Result<AgentTaskDetailVO> detail(@PathVariable Long agentTaskId) {
    return Result.success(agentTaskService.getDetail(agentTaskId));
  }

  @GetMapping("/tasks")
  public Result<PageResult<AgentTaskSummaryVO>> page(
      @Valid @ModelAttribute AgentTaskQueryRequest request) {
    return Result.success(agentTaskService.page(request));
  }

  @GetMapping("/tasks/{agentTaskId}/steps")
  public Result<List<AgentStepVO>> steps(@PathVariable Long agentTaskId) {
    return Result.success(agentTaskService.listSteps(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/findings")
  public Result<List<AgentFindingVO>> findings(@PathVariable Long agentTaskId) {
    return Result.success(agentTaskService.listFindings(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/actions")
  public Result<List<AgentActionVO>> actions(@PathVariable Long agentTaskId) {
    return Result.success(agentTaskService.listPendingActions(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/wait-status")
  public Result<AgentWaitStatusVO> waitStatus(@PathVariable Long agentTaskId) {
    return Result.success(waitStatusQueryService.get(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/reanalysis")
  public Result<AgentReanalysisComparisonVO> reanalysis(@PathVariable Long agentTaskId) {
    return Result.success(reanalysisQueryService.latest(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/analysis-rounds")
  public Result<List<AgentAnalysisRoundVO>> analysisRounds(@PathVariable Long agentTaskId) {
    return Result.success(reanalysisQueryService.rounds(agentTaskId));
  }

  @PostMapping("/tasks/{agentTaskId}/archive/prepare")
  public Result<AgentDigitalArchiveResultVO> prepareArchive(@PathVariable Long agentTaskId) {
    return Result.success(digitalArchiveService.prepare(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/archive/status")
  public Result<AgentDigitalArchiveStatusVO> archiveStatus(@PathVariable Long agentTaskId) {
    return Result.success(digitalArchiveService.status(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/archive/result")
  public Result<AgentDigitalArchiveResultVO> archiveResult(@PathVariable Long agentTaskId) {
    return Result.success(digitalArchiveService.result(agentTaskId));
  }

  @PostMapping("/tasks/{agentTaskId}/start")
  public Result<AgentWorkflowStartVO> start(@PathVariable Long agentTaskId) {
    return Result.success(workflowRunner.start(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/diagnosis")
  public Result<AgentDiagnosisResponseVO> diagnosis(@PathVariable Long agentTaskId) {
    return Result.success(workflowRunner.getDiagnosis(agentTaskId));
  }

  @GetMapping("/tasks/{agentTaskId}/evidence-analysis")
  public Result<AgentEvidenceAnalysisResponseVO> evidenceAnalysis(@PathVariable Long agentTaskId) {
    return Result.success(evidenceAnalysisRunner.getAnalysis(agentTaskId));
  }

  @PostMapping("/tasks/{agentTaskId}/collection-plan/generate")
  @RequirePermission("growth:record:create")
  public Result<AgentCollectionPlanVO> generateCollectionPlan(
      @PathVariable Long agentTaskId, @RequestParam(defaultValue = "false") boolean regenerate) {
    return Result.success(collectionPlanService.generate(agentTaskId, regenerate));
  }

  @GetMapping("/tasks/{agentTaskId}/collection-plan")
  public Result<AgentCollectionPlanVO> collectionPlan(@PathVariable Long agentTaskId) {
    return Result.success(collectionPlanService.get(agentTaskId));
  }

  @PutMapping("/tasks/{agentTaskId}/collection-plan/{planId}")
  @RequirePermission("growth:record:create")
  public Result<AgentCollectionPlanVO> updateCollectionPlan(
      @PathVariable Long agentTaskId,
      @PathVariable Long planId,
      @Valid @RequestBody AgentCollectionPlanUpdateRequest request) {
    return Result.success(collectionPlanService.update(agentTaskId, planId, request));
  }

  @PostMapping("/actions/{actionId}/confirm")
  @RequirePermission("growth:record:create")
  public Result<AgentActionExecutionVO> confirmAction(
      @PathVariable Long actionId, @Valid @RequestBody AgentActionConfirmRequest request) {
    return Result.success(actionExecutionDispatcher.confirm(actionId, request));
  }

  @PostMapping("/actions/{actionId}/reject")
  @RequirePermission("growth:record:create")
  public Result<AgentActionExecutionVO> rejectAction(
      @PathVariable Long actionId, @Valid @RequestBody AgentActionRejectRequest request) {
    return Result.success(actionExecutionDispatcher.reject(actionId, request));
  }

  @PostMapping("/tasks/{agentTaskId}/cancel")
  public Result<AgentTaskDetailVO> cancel(
      @PathVariable Long agentTaskId, @Valid @RequestBody AgentTaskCancelRequest request) {
    return Result.success(agentTaskService.cancel(agentTaskId, request));
  }
}
