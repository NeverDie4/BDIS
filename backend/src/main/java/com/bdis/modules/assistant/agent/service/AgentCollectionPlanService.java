package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentCollectionPlanUpdateRequest;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.service.FollowUpCollectionPlanGenerator.GeneratedPlan;
import com.bdis.modules.assistant.agent.service.FollowUpCollectionPlanGenerator.PlanInput;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutionContext;
import com.bdis.modules.assistant.agent.tool.AgentToolExecutor;
import com.bdis.modules.assistant.agent.tool.AgentToolResult;
import com.bdis.modules.assistant.agent.tool.CollectionTaskReadTool;
import com.bdis.modules.assistant.agent.tool.dto.CollectionTaskToolData;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.assistant.agent.vo.AgentEvidenceAnalysisResponseVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentCollectionPlanService {

  private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
  private static final Map<String, String> STATUS_LABELS =
      Map.ofEntries(
          Map.entry("DRAFT", "草稿"),
          Map.entry("PROPOSED", "待确认"),
          Map.entry("CONFIRMED", "已确认"),
          Map.entry("REJECTED", "已拒绝"),
          Map.entry("CREATED", "已创建采集任务"),
          Map.entry("CANCELLED", "已取消"),
          Map.entry("EXPIRED", "已过期"));

  private final AgentCollectionPlanMapper planMapper;
  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentActionMapper actionMapper;
  private final HerbDigitalTwinAgentTaskService taskService;
  private final HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner;
  private final AgentTaskStateMachine stateMachine;
  private final AgentToolExecutor toolExecutor;
  private final CollectionTaskReadTool collectionTaskTool;
  private final FollowUpCollectionPlanGenerator generator;
  private final ObjectMapper objectMapper;
  private final ConcurrentHashMap<Long, ReentrantLock> taskLocks = new ConcurrentHashMap<>();

  public AgentCollectionPlanService(
      AgentCollectionPlanMapper planMapper,
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentActionMapper actionMapper,
      HerbDigitalTwinAgentTaskService taskService,
      HerbDigitalTwinEvidenceAnalysisRunner evidenceAnalysisRunner,
      AgentTaskStateMachine stateMachine,
      AgentToolExecutor toolExecutor,
      CollectionTaskReadTool collectionTaskTool,
      FollowUpCollectionPlanGenerator generator,
      ObjectMapper objectMapper) {
    this.planMapper = planMapper;
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.actionMapper = actionMapper;
    this.taskService = taskService;
    this.evidenceAnalysisRunner = evidenceAnalysisRunner;
    this.stateMachine = stateMachine;
    this.toolExecutor = toolExecutor;
    this.collectionTaskTool = collectionTaskTool;
    this.generator = generator;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public AgentCollectionPlanVO generate(Long agentTaskId, boolean regenerate) {
    taskService.getDetail(agentTaskId);
    ReentrantLock lock = taskLocks.computeIfAbsent(agentTaskId, ignored -> new ReentrantLock());
    lock.lock();
    try {
      return generateLocked(agentTaskId, regenerate);
    } finally {
      lock.unlock();
      if (!lock.hasQueuedThreads()) {
        taskLocks.remove(agentTaskId, lock);
      }
    }
  }

  public AgentCollectionPlanVO get(Long agentTaskId) {
    taskService.getDetail(agentTaskId);
    AgentCollectionPlanEntity plan = planMapper.selectLatestActive(agentTaskId);
    if (plan == null) {
      throw new BusinessException("当前 Agent 任务尚未生成有效复测方案");
    }
    return toVo(plan);
  }

  @Transactional
  public AgentCollectionPlanVO update(
      Long agentTaskId, Long planId, AgentCollectionPlanUpdateRequest request) {
    taskService.getDetail(agentTaskId);
    AgentCollectionPlanEntity existing = planMapper.selectById(planId);
    if (existing == null || !agentTaskId.equals(existing.getAgentTaskId())) {
      throw new BusinessException("复测方案不存在或不属于当前 Agent 任务");
    }
    validateEditable(existing, request);
    AgentCollectionPlanEntity update = new AgentCollectionPlanEntity();
    update.setId(existing.getId());
    update.setAgentTaskId(existing.getAgentTaskId());
    update.setObjective(request.objective().trim());
    update.setRecommendedTimeType("MANUAL_WINDOW");
    update.setRecommendedAfterDays(null);
    update.setRecommendedStartTime(request.recommendedStartTime());
    update.setRecommendedEndTime(request.recommendedEndTime());
    update.setRequiredMetricsJson(toJson(request.requiredMetrics()));
    update.setRequiredImagesJson(toJson(request.requiredImages()));
    update.setCompletionCriteriaJson(toJson(request.completionCriteria()));
    update.setRationale(request.rationale().trim());
    update.setUpdateTime(LocalDateTime.now());
    if (planMapper.updateEditable(update, existing.getVersion()) == 0) {
      throw new ResourceConflictException("复测方案已被其他请求更新，请刷新后重试");
    }
    return toVo(planMapper.selectById(planId));
  }

  private AgentCollectionPlanVO generateLocked(Long agentTaskId, boolean regenerate) {
    AgentCollectionPlanEntity active = planMapper.selectLatestActive(agentTaskId);
    if (active != null && !regenerate) {
      return toVo(active);
    }
    AgentTaskEntity task = taskMapper.selectById(agentTaskId);
    requireGenerationState(task, regenerate);
    AgentEvidenceAnalysisResponseVO analysisResponse =
        evidenceAnalysisRunner.getAnalysis(agentTaskId);
    if (!Boolean.TRUE.equals(analysisResponse.available()) || analysisResponse.analysis() == null) {
      throw new BusinessException("证据分析尚未完成，不能生成复测方案");
    }
    int round = value(planMapper.selectMaxRound(agentTaskId)) + 1;
    if (regenerate) {
      LocalDateTime now = LocalDateTime.now();
      planMapper.cancelActive(agentTaskId, now);
      actionMapper.cancelPendingCollectionPlanAction(agentTaskId, now);
    }
    int generateStepNo = 7 + (round - 1) * 2;
    int waitStepNo = generateStepNo + 1;
    ensureStep(
        agentTaskId,
        generateStepNo,
        "GENERATE_COLLECTION_PLAN",
        "生成下一轮复测采集方案",
        "基于证据缺口生成白名单约束的待确认方案");
    ensureStep(agentTaskId, waitStepNo, "WAIT_FOR_CONFIRMATION", "等待用户确认复测方案", "只等待确认，不创建真实采集任务");
    AgentStepEntity generateStep = stepMapper.selectByTaskIdAndNo(agentTaskId, generateStepNo);
    beginStep(generateStep);
    AgentToolExecutionContext context = context(task, generateStep);
    CollectionTaskToolData.Overview overview =
        invoke(
            CollectionTaskReadTool.OVERVIEW,
            context,
            "读取复测方案所需任务摘要",
            () ->
                collectionTaskTool.getCollectionTaskOverview(task.getCollectionTaskId(), context));
    List<AgentFindingVO> findings = taskService.listFindings(agentTaskId);
    GeneratedPlan generated =
        generator.generate(
            new PlanInput(agentTaskId, overview, analysisResponse.analysis(), findings));
    AgentCollectionPlanEntity entity = toEntity(task, generateStep, round, overview, generated);
    planMapper.insert(entity);
    entity.setPlanNo(planNo(entity.getCreateTime(), agentTaskId, round));
    // plan_no 在插入前已按任务与轮次生成；保留这里的实体值便于构建动作摘要。
    createPendingAction(task, generateStep, entity, generated.plan());
    completeSteps(generateStep, waitStepNo, generated.plan());
    if (AgentTaskStatus.RUNNING.getCode().equals(task.getStatus())) {
      stateMachine.transition(task, AgentTaskStatus.WAITING_CONFIRMATION);
    }
    taskService.updateProgress(agentTaskId, 65, "WAITING_COLLECTION_PLAN_CONFIRMATION");
    return toVo(planMapper.selectById(entity.getId()));
  }

  private AgentCollectionPlanEntity toEntity(
      AgentTaskEntity task,
      AgentStepEntity step,
      int round,
      CollectionTaskToolData.Overview overview,
      GeneratedPlan generated) {
    FollowUpCollectionPlan plan = generated.plan();
    LocalDateTime now = LocalDateTime.now();
    AgentCollectionPlanEntity entity = new AgentCollectionPlanEntity();
    entity.setPlanNo(planNo(now, task.getId(), round));
    entity.setAgentTaskId(task.getId());
    entity.setSourceStepId(step.getId());
    entity.setResearchRound(round);
    entity.setCollectionTaskId(task.getCollectionTaskId());
    entity.setSpeciesId(overview.speciesId());
    entity.setBaseId(overview.baseId());
    entity.setObjective(plan.objective());
    entity.setRecommendedTimeType(plan.recommendedTimeType());
    entity.setRecommendedAfterDays(plan.recommendedAfterDays());
    entity.setRecommendedStartTime(plan.recommendedStartTime());
    entity.setRecommendedEndTime(plan.recommendedEndTime());
    entity.setRequiredMetricsJson(toJson(plan.requiredMetrics()));
    entity.setRequiredImagesJson(toJson(plan.requiredImages()));
    entity.setOptionalItemsJson(toJson(plan.optionalItems()));
    entity.setCompletionCriteriaJson(toJson(plan.completionCriteria()));
    entity.setSourceFindingsJson(toJson(plan.sourceFindingIds()));
    entity.setRationale(plan.rationale());
    entity.setUncertainty(plan.uncertainty());
    entity.setPriority(plan.priority());
    entity.setPlanSource(generated.planSource());
    entity.setPromptVersion(generated.promptVersion());
    entity.setModelName(generated.modelName());
    entity.setStatus("PROPOSED");
    entity.setVersion(0);
    entity.setCreateTime(now);
    entity.setUpdateTime(now);
    return entity;
  }

  private void createPendingAction(
      AgentTaskEntity task,
      AgentStepEntity step,
      AgentCollectionPlanEntity plan,
      FollowUpCollectionPlan content) {
    LocalDateTime now = LocalDateTime.now();
    AgentActionEntity action = new AgentActionEntity();
    action.setAgentTaskId(task.getId());
    action.setStepId(step.getId());
    action.setActionType("CREATE_FOLLOW_UP_COLLECTION_TASK");
    action.setTargetType("AGENT_COLLECTION_PLAN");
    action.setTargetId(plan.getId());
    action.setActionName("确认并创建下一轮复测采集任务");
    action.setActionDescription("用户确认后才可进入真实采集任务创建流程，本阶段不会执行创建。 ");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("planId", plan.getId());
    payload.put("planNo", plan.getPlanNo());
    payload.put("objective", truncate(content.objective(), 240));
    action.setPayloadJson(toJson(payload));
    action.setRiskLevel("MEDIUM");
    action.setNeedConfirm(1);
    action.setStatus("WAITING_CONFIRMATION");
    action.setVersion(0);
    action.setRequestedTime(now);
    action.setCreateTime(now);
    action.setUpdateTime(now);
    actionMapper.insert(action);
  }

  private void completeSteps(
      AgentStepEntity generateStep, int waitStepNo, FollowUpCollectionPlan plan) {
    LocalDateTime now = LocalDateTime.now();
    if (stepMapper.markSucceeded(generateStep.getId(), "复测采集方案已生成并等待确认", toJson(plan), now) == 0) {
      throw new ResourceConflictException("复测方案步骤完成状态更新冲突");
    }
    AgentStepEntity waitStep =
        stepMapper.selectByTaskIdAndNo(generateStep.getAgentTaskId(), waitStepNo);
    if (waitStep == null || stepMapper.markWaiting(waitStep.getId(), now) == 0) {
      throw new ResourceConflictException("等待确认步骤状态更新冲突");
    }
  }

  private void ensureStep(Long taskId, int stepNo, String type, String name, String description) {
    LocalDateTime now = LocalDateTime.now();
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(taskId);
    step.setStepNo(stepNo);
    step.setStepType(type);
    step.setStepName(name);
    step.setDescription(description);
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    stepMapper.insertIfAbsent(step);
  }

  private void beginStep(AgentStepEntity step) {
    if (step == null || stepMapper.markRunning(step.getId(), LocalDateTime.now()) == 0) {
      throw new ResourceConflictException("复测方案生成步骤已由其他执行器推进");
    }
  }

  private void requireGenerationState(AgentTaskEntity task, boolean regenerate) {
    if (task == null) {
      throw new BusinessException("Agent 任务不存在");
    }
    boolean initial =
        AgentTaskStatus.RUNNING.getCode().equals(task.getStatus())
            && "EVIDENCE_ANALYSIS_COMPLETED".equals(task.getCurrentPhase());
    boolean regeneration =
        regenerate && AgentTaskStatus.WAITING_CONFIRMATION.getCode().equals(task.getStatus());
    if (!initial && !regeneration) {
      throw new BusinessException("只有证据分析完成后才能生成复测方案");
    }
  }

  private void validateEditable(
      AgentCollectionPlanEntity existing, AgentCollectionPlanUpdateRequest request) {
    if (!Set.of("DRAFT", "PROPOSED").contains(existing.getStatus())) {
      throw new BusinessException("当前复测方案状态不允许调整");
    }
    if (request.recommendedStartTime() != null
        && request.recommendedStartTime().isBefore(LocalDateTime.now())) {
      throw new BusinessException("推荐开始时间不能早于当前时间");
    }
    if (request.recommendedStartTime() != null
        && request.recommendedEndTime() != null
        && request.recommendedEndTime().isBefore(request.recommendedStartTime())) {
      throw new BusinessException("推荐结束时间不能早于开始时间");
    }
    boolean illegalMetric =
        request.requiredMetrics().stream()
            .anyMatch(
                item ->
                    item == null
                        || !FollowUpCollectionPlanGenerator.METRIC_WHITELIST.contains(
                            item.metricCode()));
    boolean illegalImage =
        request.requiredImages().stream()
            .anyMatch(
                item ->
                    item == null
                        || !FollowUpCollectionPlanGenerator.IMAGE_WHITELIST.contains(
                            item.imageType())
                        || item.minCount() == null
                        || item.minCount() < 1
                        || item.minCount() > 5);
    if (illegalMetric || illegalImage) {
      throw new BusinessException("复测方案包含不支持的指标、图片类型或数量");
    }
  }

  private AgentCollectionPlanVO toVo(AgentCollectionPlanEntity entity) {
    if (entity == null) {
      throw new BusinessException("复测方案读取失败");
    }
    FollowUpCollectionPlan plan =
        new FollowUpCollectionPlan(
            entity.getObjective(),
            entity.getRecommendedTimeType(),
            entity.getRecommendedAfterDays(),
            entity.getRecommendedStartTime(),
            entity.getRecommendedEndTime(),
            read(entity.getRequiredMetricsJson(), new TypeReference<>() {}),
            read(entity.getRequiredImagesJson(), new TypeReference<>() {}),
            read(entity.getOptionalItemsJson(), new TypeReference<>() {}),
            read(entity.getCompletionCriteriaJson(), new TypeReference<>() {}),
            read(entity.getSourceFindingsJson(), new TypeReference<>() {}),
            entity.getRationale(),
            entity.getUncertainty(),
            entity.getPriority());
    return new AgentCollectionPlanVO(
        entity.getId(),
        entity.getPlanNo(),
        entity.getAgentTaskId(),
        entity.getResearchRound(),
        entity.getCollectionTaskId(),
        entity.getStatus(),
        STATUS_LABELS.getOrDefault(entity.getStatus(), entity.getStatus()),
        entity.getPlanSource(),
        plan,
        entity.getCreateTime(),
        entity.getUpdateTime());
  }

  private AgentToolExecutionContext context(AgentTaskEntity task, AgentStepEntity step) {
    return AgentToolExecutionContext.fromCurrentUser(
        new AgentToolExecutionContext.Scope(
            task.getSessionId(),
            task.getId(),
            step.getId(),
            "digital-life",
            task.getCollectionTaskId(),
            null,
            null,
            null,
            UUID.randomUUID().toString()));
  }

  private CollectionTaskToolData.Overview invoke(
      String toolName,
      AgentToolExecutionContext context,
      String summary,
      AgentToolExecutor.AgentToolInvocation<CollectionTaskToolData.Overview> invocation) {
    AgentToolResult<CollectionTaskToolData.Overview> result =
        toolExecutor.execute(toolName, context, summary, invocation);
    if (!Boolean.TRUE.equals(result.success())) {
      throw new BusinessException(result.summary());
    }
    return result.data();
  }

  private String planNo(LocalDateTime time, Long taskId, int round) {
    return "PLAN-DT-"
        + DATE_FORMAT.format(time.toLocalDate())
        + "-"
        + taskId
        + "-"
        + String.format("%02d", round);
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("复测方案序列化失败");
    }
  }

  private <T> T read(String json, TypeReference<T> type) {
    if (!StringUtils.hasText(json)) {
      try {
        return objectMapper.readValue("[]", type);
      } catch (JsonProcessingException exception) {
        throw new BusinessException("复测方案读取失败");
      }
    }
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("复测方案读取失败");
    }
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }

  private String truncate(String value, int length) {
    if (!StringUtils.hasText(value) || value.length() <= length) {
      return value;
    }
    return value.substring(0, length);
  }
}
