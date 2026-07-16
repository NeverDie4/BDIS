package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentActionConfirmRequest;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ConditionDefinition;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ImageRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.MetricRequirement;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredImageItem;
import com.bdis.modules.assistant.agent.dto.FollowUpCollectionPlan.RequiredMetricItem;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentBusinessLinkEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionPlanEntity;
import com.bdis.modules.assistant.agent.entity.AgentCollectionRequirementEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentBusinessLinkMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionPlanMapper;
import com.bdis.modules.assistant.agent.mapper.AgentCollectionRequirementMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import com.bdis.modules.assistant.agent.vo.AgentCollectionPlanVO;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.service.HerbCollectionTaskService;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
public class AgentActionConfirmationService {

  private static final String ACTION_TYPE = "CREATE_FOLLOW_UP_COLLECTION_TASK";
  private static final String RELATION_TYPE = "FOLLOW_UP_COLLECTION_TASK";

  private final AgentActionMapper actionMapper;
  private final AgentTaskMapper taskMapper;
  private final AgentCollectionPlanMapper planMapper;
  private final AgentBusinessLinkMapper businessLinkMapper;
  private final AgentCollectionRequirementMapper requirementMapper;
  private final AgentStepMapper stepMapper;
  private final AgentWaitConditionMapper waitConditionMapper;
  private final AgentFieldDataConditionEvaluator fieldDataConditionEvaluator;
  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final AgentCollectionPlanService collectionPlanService;
  private final HerbCollectionTaskService collectionTaskService;
  private final AgentTaskStateMachine stateMachine;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transaction;
  private final TransactionTemplate failureTransaction;
  private final ThreadLocal<Boolean> executionStarted = ThreadLocal.withInitial(() -> false);

  public AgentActionConfirmationService(
      AgentActionMapper actionMapper,
      AgentTaskMapper taskMapper,
      AgentCollectionPlanMapper planMapper,
      AgentBusinessLinkMapper businessLinkMapper,
      AgentCollectionRequirementMapper requirementMapper,
      AgentStepMapper stepMapper,
      AgentWaitConditionMapper waitConditionMapper,
      AgentFieldDataConditionEvaluator fieldDataConditionEvaluator,
      HerbDigitalTwinAgentTaskService agentTaskService,
      AgentCollectionPlanService collectionPlanService,
      HerbCollectionTaskService collectionTaskService,
      AgentTaskStateMachine stateMachine,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {
    this.actionMapper = actionMapper;
    this.taskMapper = taskMapper;
    this.planMapper = planMapper;
    this.businessLinkMapper = businessLinkMapper;
    this.requirementMapper = requirementMapper;
    this.stepMapper = stepMapper;
    this.waitConditionMapper = waitConditionMapper;
    this.fieldDataConditionEvaluator = fieldDataConditionEvaluator;
    this.agentTaskService = agentTaskService;
    this.collectionPlanService = collectionPlanService;
    this.collectionTaskService = collectionTaskService;
    this.stateMachine = stateMachine;
    this.objectMapper = objectMapper;
    this.transaction = new TransactionTemplate(transactionManager);
    this.failureTransaction = new TransactionTemplate(transactionManager);
    this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public AgentActionExecutionVO confirm(Long actionId, AgentActionConfirmRequest request) {
    AgentBusinessLinkEntity existing = businessLinkMapper.selectByAction(actionId, RELATION_TYPE);
    if (existing != null) {
      return existingResult(actionId, existing);
    }
    try {
      return transaction.execute(
          status ->
              executeConfirmation(
                  actionId, request != null && Boolean.TRUE.equals(request.publishAfterCreate())));
    } catch (RuntimeException exception) {
      if (Boolean.TRUE.equals(executionStarted.get())) {
        failureTransaction.executeWithoutResult(status -> recordFailure(actionId, exception));
      }
      throw exception;
    } finally {
      executionStarted.remove();
    }
  }

  public AgentActionExecutionVO reject(Long actionId, AgentActionRejectRequest request) {
    return transaction.execute(status -> executeRejection(actionId, request.reason()));
  }

  private AgentActionExecutionVO executeConfirmation(Long actionId, boolean publishAfterCreate) {
    AgentBusinessLinkEntity existing = businessLinkMapper.selectByAction(actionId, RELATION_TYPE);
    if (existing != null) {
      return existingResult(actionId, existing);
    }
    AgentActionEntity action = requireAction(actionId);
    AgentTaskEntity agentTask = requireManageableTask(action);
    AgentCollectionPlanEntity plan = requireValidPlan(action);
    LocalDateTime now = LocalDateTime.now();
    if (actionMapper.confirm(
            actionId, action.getVersion(), SecurityUtils.currentUser().getUserId(), now)
        == 0) {
      throw new ResourceConflictException("动作已被确认或状态已变化");
    }
    if (planMapper.updateStatus(plan.getId(), "PROPOSED", "CONFIRMED", plan.getVersion(), now)
        == 0) {
      throw new ResourceConflictException("复测方案状态已变化");
    }
    if (actionMapper.markExecuting(actionId, action.getVersion() + 1, now) == 0) {
      throw new ResourceConflictException("动作执行状态更新冲突");
    }
    completeConfirmationStep(agentTask.getId(), "用户已确认复测采集方案", now);
    executionStarted.set(true);

    AgentCollectionPlanVO planView = collectionPlanService.get(agentTask.getId());
    HerbCollectionTaskVO sourceTask = collectionTaskService.getById(plan.getCollectionTaskId());
    HerbCollectionTaskVO created =
        collectionTaskService.create(createRequest(actionId, sourceTask, planView.plan()));
    if (publishAfterCreate) {
      created = collectionTaskService.publish(created.getId());
    }
    persistRequirements(plan, created.getId(), planView.plan());
    persistBusinessLink(agentTask, action, plan, created);
    AgentStepEntity waitStep = createWorkflowSteps(agentTask, created);
    persistWaitCondition(agentTask, plan, created, waitStep, planView.plan());

    if (planMapper.updateStatus(plan.getId(), "CONFIRMED", "CREATED", plan.getVersion() + 1, now)
        == 0) {
      throw new ResourceConflictException("复测方案创建状态更新冲突");
    }
    if (actionMapper.markSucceeded(
            actionId, action.getVersion() + 2, "已创建复测采集任务 " + created.getTaskCode(), now)
        == 0) {
      throw new ResourceConflictException("动作成功状态更新冲突");
    }
    AgentTaskEntity running = stateMachine.transition(agentTask, AgentTaskStatus.RUNNING);
    stateMachine.transition(running, AgentTaskStatus.WAITING_FIELD_DATA);
    agentTaskService.updateProgress(agentTask.getId(), 72, "WAITING_FOR_FOLLOW_UP_DATA");
    return new AgentActionExecutionVO(
        actionId,
        "SUCCEEDED",
        plan.getId(),
        created.getId(),
        created.getTaskCode(),
        created.getTaskStatus(),
        "复测采集任务已创建，等待现场数据回传。");
  }

  private AgentActionExecutionVO executeRejection(Long actionId, String reason) {
    AgentActionEntity action = requireAction(actionId);
    requireManageableTask(action);
    AgentCollectionPlanEntity plan = requireValidPlan(action);
    LocalDateTime now = LocalDateTime.now();
    if (actionMapper.reject(actionId, action.getVersion(), truncate(reason, 500), now) == 0) {
      throw new ResourceConflictException("动作已处理或状态已变化");
    }
    if (planMapper.updateStatus(plan.getId(), "PROPOSED", "REJECTED", plan.getVersion(), now)
        == 0) {
      throw new ResourceConflictException("复测方案状态已变化");
    }
    completeConfirmationStep(action.getAgentTaskId(), "用户已拒绝本次复测采集方案", now);
    return new AgentActionExecutionVO(
        actionId, "REJECTED", plan.getId(), null, null, null, "已拒绝本次复测方案，未创建采集任务。");
  }

  private AgentActionEntity requireAction(Long actionId) {
    AgentActionEntity action = actionMapper.selectById(actionId);
    if (action == null) {
      throw new BusinessException("待确认动作不存在");
    }
    if (!ACTION_TYPE.equals(action.getActionType())) {
      throw new BusinessException("当前动作类型不支持此确认流程");
    }
    if (!"WAITING_CONFIRMATION".equals(action.getStatus())) {
      throw new BusinessException("当前动作状态不允许确认或拒绝");
    }
    return action;
  }

  private AgentTaskEntity requireManageableTask(AgentActionEntity action) {
    CurrentUser current = SecurityUtils.currentUser();
    if (current.getRoleCodes().stream()
        .noneMatch(role -> "ADMIN".equals(role) || "TEACHER".equals(role))) {
      throw new ForbiddenException("只有管理员或教师可以确认创建复测采集任务");
    }
    agentTaskService.getDetail(action.getAgentTaskId());
    AgentTaskEntity task = taskMapper.selectById(action.getAgentTaskId());
    if (task == null || !AgentTaskStatus.WAITING_CONFIRMATION.getCode().equals(task.getStatus())) {
      throw new BusinessException("Agent 任务当前不在等待确认状态");
    }
    return task;
  }

  private AgentCollectionPlanEntity requireValidPlan(AgentActionEntity action) {
    AgentCollectionPlanEntity plan = planMapper.selectById(action.getTargetId());
    if (plan == null
        || !action.getAgentTaskId().equals(plan.getAgentTaskId())
        || !"PROPOSED".equals(plan.getStatus())) {
      throw new BusinessException("复测方案不存在、已失效或不属于当前 Agent 任务");
    }
    if (plan.getExpireTime() != null && !plan.getExpireTime().isAfter(LocalDateTime.now())) {
      throw new BusinessException("复测方案已过期，请重新生成");
    }
    return plan;
  }

  private HerbCollectionTaskCreateRequest createRequest(
      Long actionId, HerbCollectionTaskVO source, FollowUpCollectionPlan plan) {
    HerbCollectionTaskCreateRequest request = new HerbCollectionTaskCreateRequest();
    request.setTaskCode("AGENT-FU-" + actionId);
    request.setTaskName(source.getTaskName() + "重点复测");
    request.setSpeciesId(source.getSpeciesId());
    request.setSpeciesName(source.getSpeciesName());
    request.setBaseId(source.getBaseId());
    request.setBaseName(source.getBaseName());
    request.setCollectPlace(source.getCollectPlace());
    request.setPlannedStartTime(plan.recommendedStartTime());
    request.setPlannedEndTime(plan.recommendedEndTime());
    request.setCollectorId(source.getCollectorId());
    request.setCollectorName(source.getCollectorName());
    request.setTaskStatus(HerbCollectionTaskStatusConstants.DRAFT);
    request.setDescription(
        truncate(
            plan.objective()
                + "\n方案依据："
                + plan.rationale()
                + "\n完成条件："
                + String.join("；", plan.completionCriteria()),
            4000));
    request.setRemark("由本草数字孪生科研 Agent 方案经用户确认后创建，未包含真实采集结果。");
    return request;
  }

  private void persistRequirements(
      AgentCollectionPlanEntity planEntity, Long taskId, FollowUpCollectionPlan plan) {
    int order = 1;
    for (RequiredMetricItem metric : plan.requiredMetrics()) {
      requirementMapper.insert(
          requirement(
              planEntity.getId(),
              taskId,
              "METRIC",
              metric.metricCode(),
              metric.metricName(),
              Boolean.TRUE.equals(metric.required()),
              null,
              metric.unit(),
              metric.inputHint(),
              metric.reason(),
              order++));
    }
    for (RequiredImageItem image : plan.requiredImages()) {
      requirementMapper.insert(
          requirement(
              planEntity.getId(),
              taskId,
              "IMAGE",
              image.imageType(),
              image.imageTypeName(),
              true,
              image.minCount(),
              null,
              image.shootingGuidance(),
              image.reason(),
              order++));
    }
    int criterion = 1;
    for (String text : plan.completionCriteria()) {
      requirementMapper.insert(
          requirement(
              planEntity.getId(),
              taskId,
              "NOTE",
              "completion_" + criterion++,
              "完成条件",
              true,
              null,
              null,
              text,
              null,
              order++));
    }
  }

  private AgentCollectionRequirementEntity requirement(
      Long planId,
      Long taskId,
      String type,
      String code,
      String name,
      boolean required,
      Integer minCount,
      String unit,
      String guidance,
      String reason,
      int order) {
    AgentCollectionRequirementEntity entity = new AgentCollectionRequirementEntity();
    entity.setCollectionPlanId(planId);
    entity.setCollectionTaskId(taskId);
    entity.setRequirementType(type);
    entity.setRequirementCode(code);
    entity.setRequirementName(name);
    entity.setRequired(required ? 1 : 0);
    entity.setMinCount(minCount);
    entity.setUnit(unit);
    entity.setGuidance(guidance);
    entity.setReason(reason);
    entity.setSortOrder(order);
    entity.setCreateTime(LocalDateTime.now());
    return entity;
  }

  private void persistBusinessLink(
      AgentTaskEntity task,
      AgentActionEntity action,
      AgentCollectionPlanEntity plan,
      HerbCollectionTaskVO created) {
    AgentBusinessLinkEntity link = new AgentBusinessLinkEntity();
    link.setAgentTaskId(task.getId());
    link.setAgentActionId(action.getId());
    link.setCollectionPlanId(plan.getId());
    link.setRelationType(RELATION_TYPE);
    link.setBusinessType("HERB_COLLECTION_TASK");
    link.setBusinessId(created.getId());
    link.setBusinessNo(created.getTaskCode());
    link.setCreateTime(LocalDateTime.now());
    businessLinkMapper.insert(link);
  }

  private AgentStepEntity createWorkflowSteps(AgentTaskEntity task, HerbCollectionTaskVO created) {
    int next = value(stepMapper.selectMaxStepNo(task.getId())) + 1;
    AgentStepEntity createStep =
        insertStep(task.getId(), next, "CREATE_COLLECTION_TASK", "创建真实复测采集任务");
    if (stepMapper.markRunning(createStep.getId(), LocalDateTime.now()) == 0
        || stepMapper.markSucceeded(
                createStep.getId(),
                "已通过现有采集任务 Service 创建草稿任务",
                toJson(
                    new AgentActionExecutionVO(
                        null,
                        "SUCCEEDED",
                        null,
                        created.getId(),
                        created.getTaskCode(),
                        created.getTaskStatus(),
                        null)),
                LocalDateTime.now())
            == 0) {
      throw new ResourceConflictException("创建采集任务步骤状态更新冲突");
    }
    AgentStepEntity waitStep =
        insertStep(task.getId(), next + 1, "WAIT_FOR_FIELD_DATA", "等待移动端现场数据");
    if (stepMapper.markWaiting(waitStep.getId(), LocalDateTime.now()) == 0) {
      throw new ResourceConflictException("等待现场数据步骤状态更新冲突");
    }
    return waitStep;
  }

  private void persistWaitCondition(
      AgentTaskEntity task,
      AgentCollectionPlanEntity planEntity,
      HerbCollectionTaskVO followUpTask,
      AgentStepEntity waitStep,
      FollowUpCollectionPlan plan) {
    LocalDateTime now = LocalDateTime.now();
    ConditionDefinition definition =
        new ConditionDefinition(
            plan.requiredMetrics().stream()
                .filter(item -> Boolean.TRUE.equals(item.required()))
                .map(item -> new MetricRequirement(item.metricCode(), item.metricName()))
                .toList(),
            plan.requiredImages().stream()
                .map(
                    item ->
                        new ImageRequirement(
                            item.imageType(), item.imageTypeName(), item.minCount()))
                .toList(),
            true,
            planEntity.getCreateTime());
    AgentWaitConditionEntity condition = new AgentWaitConditionEntity();
    condition.setAgentTaskId(task.getId());
    condition.setAgentStepId(waitStep.getId());
    condition.setCollectionPlanId(planEntity.getId());
    condition.setFollowUpTaskId(followUpTask.getId());
    condition.setConditionType("COMPOSITE_FIELD_DATA_READY");
    condition.setConditionJson(toJson(definition));
    Snapshot initialSnapshot = fieldDataConditionEvaluator.evaluate(followUpTask.getId(), definition);
    condition.setCurrentSnapshotJson(toJson(initialSnapshot));
    condition.setStatus(
        initialSnapshot.satisfied()
            ? "SATISFIED"
            : initialSnapshot.completedRequirements().isEmpty()
                ? "WAITING"
                : "PARTIALLY_SATISFIED");
    condition.setDeadline(
        plan.recommendedEndTime() == null ? now.plusDays(30) : plan.recommendedEndTime());
    condition.setLastCheckTime(now);
    condition.setCheckCount(1);
    condition.setVersion(0);
    condition.setCreateTime(now);
    condition.setUpdateTime(now);
    waitConditionMapper.insert(condition);
  }

  private AgentStepEntity insertStep(Long taskId, int no, String type, String name) {
    LocalDateTime now = LocalDateTime.now();
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(taskId);
    step.setStepNo(no);
    step.setStepType(type);
    step.setStepName(name);
    step.setDescription(name);
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    if (stepMapper.insertIfAbsent(step) == 0) {
      step = stepMapper.selectByTaskIdAndNo(taskId, no);
    }
    if (step == null || step.getId() == null) {
      throw new ResourceConflictException("Agent 工作流步骤创建冲突");
    }
    return step;
  }

  private void completeConfirmationStep(Long taskId, String summary, LocalDateTime finishTime) {
    AgentStepEntity waitingStep =
        stepMapper.selectByTaskId(taskId).stream()
            .filter(
                step ->
                    "WAIT_FOR_CONFIRMATION".equals(step.getStepType())
                        && "WAITING".equals(step.getStatus()))
            .max(Comparator.comparing(AgentStepEntity::getStepNo))
            .orElse(null);
    if (waitingStep != null
        && stepMapper.completeWaiting(waitingStep.getId(), summary, null, finishTime) == 0) {
      throw new ResourceConflictException("等待确认步骤状态更新冲突");
    }
  }

  private AgentActionExecutionVO existingResult(Long actionId, AgentBusinessLinkEntity link) {
    HerbCollectionTaskVO task = collectionTaskService.getById(link.getBusinessId());
    return new AgentActionExecutionVO(
        actionId,
        "SUCCEEDED",
        link.getCollectionPlanId(),
        task.getId(),
        task.getTaskCode(),
        task.getTaskStatus(),
        "该动作已创建复测采集任务，本次未重复创建。");
  }

  private void recordFailure(Long actionId, RuntimeException exception) {
    AgentActionEntity action = actionMapper.selectById(actionId);
    if (action != null
        && Set.of("WAITING_CONFIRMATION", "CONFIRMED", "EXECUTING").contains(action.getStatus())) {
      actionMapper.markFailed(
          actionId,
          action.getVersion(),
          truncate(exception.getMessage(), 1000),
          LocalDateTime.now());
    }
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("Agent 动作结果序列化失败");
    }
  }

  private String truncate(String value, int maxLength) {
    if (!StringUtils.hasText(value)) {
      return "未知错误";
    }
    return value.length() <= maxLength ? value : value.substring(0, maxLength);
  }
}
