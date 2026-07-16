package com.bdis.modules.assistant.agent.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.constant.AgentDefinitions;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentTaskCancelRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskCreateRequest;
import com.bdis.modules.assistant.agent.dto.AgentTaskQueryRequest;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentFindingEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFindingMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.assistant.agent.support.AgentTaskAccessService;
import com.bdis.modules.assistant.agent.support.AgentTaskNoGenerator;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentActionVO;
import com.bdis.modules.assistant.agent.vo.AgentFindingVO;
import com.bdis.modules.assistant.agent.vo.AgentStepVO;
import com.bdis.modules.assistant.agent.vo.AgentTargetVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskSummaryVO;
import com.bdis.modules.assistant.mapper.HerbAiChatSessionMapper;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessScope;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbDigitalTwinAgentTaskServiceImpl implements HerbDigitalTwinAgentTaskService {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(HerbDigitalTwinAgentTaskServiceImpl.class);
  private static final Set<String> PENDING_ACTION_STATUSES =
      Set.of("PROPOSED", "WAITING_CONFIRMATION", "CONFIRMED");

  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentFindingMapper findingMapper;
  private final AgentActionMapper actionMapper;
  private final AgentWaitConditionMapper waitConditionMapper;
  private final HerbCollectionTaskMapper collectionTaskMapper;
  private final HerbAiChatSessionMapper chatSessionMapper;
  private final CollectionAccessService collectionAccessService;
  private final AgentTaskAccessService accessService;
  private final AgentTaskNoGenerator taskNoGenerator;
  private final AgentTaskStateMachine stateMachine;
  private final ObjectMapper objectMapper;

  public HerbDigitalTwinAgentTaskServiceImpl(
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentFindingMapper findingMapper,
      AgentActionMapper actionMapper,
      AgentWaitConditionMapper waitConditionMapper,
      HerbCollectionTaskMapper collectionTaskMapper,
      HerbAiChatSessionMapper chatSessionMapper,
      CollectionAccessService collectionAccessService,
      AgentTaskAccessService accessService,
      AgentTaskNoGenerator taskNoGenerator,
      AgentTaskStateMachine stateMachine,
      ObjectMapper objectMapper) {
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.findingMapper = findingMapper;
    this.actionMapper = actionMapper;
    this.waitConditionMapper = waitConditionMapper;
    this.collectionTaskMapper = collectionTaskMapper;
    this.chatSessionMapper = chatSessionMapper;
    this.collectionAccessService = collectionAccessService;
    this.accessService = accessService;
    this.taskNoGenerator = taskNoGenerator;
    this.stateMachine = stateMachine;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public AgentTaskSummaryVO create(AgentTaskCreateRequest request) {
    validateCreateRequest(request);
    CurrentUser current = SecurityUtils.currentUser();
    HerbCollectionTaskEntity collectionTask =
        collectionTaskMapper.selectByIdForUpdate(request.getCollectionTaskId());
    if (collectionTask == null) {
      throw new ResourceNotFoundException("采集任务不存在");
    }
    accessService.requireCreateAccess(collectionTask);
    String goalType = request.getGoalType().trim();
    AgentTaskEntity duplicate =
        taskMapper.selectActiveDuplicate(current.getUserId(), collectionTask.getId(), goalType);
    if (duplicate != null) {
      throw new ResourceConflictException("该采集任务已有正在处理的数字孪生科研 Agent 任务。");
    }

    String sessionId = validateSession(request.getSessionId(), current.getUserId());
    LocalDateTime now = LocalDateTime.now();
    AgentTaskEntity task = new AgentTaskEntity();
    String temporaryTaskNo = taskNoGenerator.temporaryTaskNo();
    task.setTaskNo(temporaryTaskNo);
    task.setSessionId(sessionId);
    task.setUserId(current.getUserId());
    task.setGoalType(goalType);
    task.setGoalText(request.getGoalText().trim());
    task.setTargetType(AgentDefinitions.DEFAULT_TARGET_TYPE);
    task.setTargetId(collectionTask.getId());
    task.setCollectionTaskId(collectionTask.getId());
    task.setSpeciesId(collectionTask.getSpeciesId());
    task.setStatus(AgentTaskStatus.CREATED.getCode());
    task.setProgressPercent(0);
    task.setContextJson(pageContextJson(request.getPageContext()));
    task.setVersion(0);
    task.setCreateTime(now);
    task.setUpdateTime(now);
    task.setDeleted(0);
    try {
      taskMapper.insert(task);
    } catch (DuplicateKeyException exception) {
      LOGGER.warn(
          "Duplicate active Agent task rejected, userId={}, collectionTaskId={}," + " goalType={}",
          current.getUserId(),
          collectionTask.getId(),
          goalType);
      throw new ResourceConflictException("该采集任务已有正在处理的数字孪生科研 Agent 任务。");
    }
    if (task.getId() == null) {
      throw new BusinessException("Agent 任务创建失败");
    }
    String taskNo = taskNoGenerator.generate(now, task.getId());
    if (taskMapper.updateTaskNo(task.getId(), temporaryTaskNo, taskNo, now) == 0) {
      throw new ResourceConflictException("Agent 任务编号生成冲突，请重试");
    }
    task.setTaskNo(taskNo);
    stepMapper.insert(initialStep(task.getId(), now));
    LOGGER.info(
        "Agent task created, taskId={}, taskNo={}, userId={}, collectionTaskId={}",
        task.getId(),
        taskNo,
        current.getUserId(),
        collectionTask.getId());
    return toSummary(task, collectionTask.getTaskName());
  }

  @Override
  public AgentTaskDetailVO getDetail(Long agentTaskId) {
    AccessibleTask accessible = requireAccessibleTask(agentTaskId);
    return toDetail(accessible.task(), accessible.collectionTask());
  }

  @Override
  public PageResult<AgentTaskSummaryVO> page(AgentTaskQueryRequest request) {
    AgentTaskQueryRequest query = request == null ? new AgentTaskQueryRequest() : request;
    normalizeQuery(query);
    CurrentUser current = SecurityUtils.currentUser();
    CollectionAccessScope scope = collectionAccessService.currentScope();
    boolean admin = accessService.isAdmin();
    boolean reviewer = accessService.isReviewer();
    long total = taskMapper.countPage(query, current.getUserId(), admin, reviewer, scope);
    long offset = (long) (query.getPage() - 1) * query.getSize();
    List<AgentTaskEntity> tasks =
        taskMapper.selectPage(
            query, current.getUserId(), admin, reviewer, scope, offset, query.getSize());
    Map<Long, String> targetNames = new HashMap<>();
    List<AgentTaskSummaryVO> records =
        tasks.stream()
            .map(
                task ->
                    toSummary(
                        task,
                        targetNames.computeIfAbsent(
                            task.getCollectionTaskId(), this::resolveTargetName)))
            .toList();
    return new PageResult<>(records, query.getPage(), query.getSize(), total);
  }

  @Override
  public List<AgentStepVO> listSteps(Long agentTaskId) {
    requireAccessibleTask(agentTaskId);
    return stepMapper.selectByTaskId(agentTaskId).stream().map(this::toStepVO).toList();
  }

  @Override
  public List<AgentFindingVO> listFindings(Long agentTaskId) {
    requireAccessibleTask(agentTaskId);
    return findingMapper.selectByTaskId(agentTaskId).stream().map(this::toFindingVO).toList();
  }

  @Override
  public List<AgentActionVO> listPendingActions(Long agentTaskId) {
    requireAccessibleTask(agentTaskId);
    return actionMapper.selectByTaskId(agentTaskId).stream()
        .filter(action -> PENDING_ACTION_STATUSES.contains(action.getStatus()))
        .map(this::toActionVO)
        .toList();
  }

  @Override
  @Transactional
  public AgentTaskDetailVO cancel(Long agentTaskId, AgentTaskCancelRequest request) {
    if (request == null || !StringUtils.hasText(request.getReason())) {
      throw new BusinessException("取消原因不能为空");
    }
    AccessibleTask accessible = requireAccessibleTask(agentTaskId);
    accessService.requireCancelAccess(accessible.task());
    AgentTaskStatus current = AgentTaskStatus.fromCode(accessible.task().getStatus());
    if (current.isTerminal()) {
      throw new ResourceConflictException("已完成、失败或已取消的 Agent 任务不能重复取消");
    }
    stateMachine.transition(
        accessible.task(), AgentTaskStatus.CANCELLED, "用户取消：" + request.getReason().trim());
    LocalDateTime now = accessible.task().getCancelTime();
    stepMapper.cancelUnfinished(agentTaskId, now);
    actionMapper.cancelUnexecuted(agentTaskId, now);
    waitConditionMapper.cancelByAgentTaskId(agentTaskId, now);
    LOGGER.info(
        "Agent task cancelled, taskId={}, userId={}",
        agentTaskId,
        SecurityUtils.currentUser().getUserId());
    return toDetail(accessible.task(), accessible.collectionTask());
  }

  @Override
  @Transactional
  public AgentTaskSummaryVO updateProgress(
      Long agentTaskId, Integer progressPercent, String currentPhase) {
    if (progressPercent == null || progressPercent < 0 || progressPercent > 100) {
      throw new BusinessException("Agent 任务进度必须在0到100之间");
    }
    AccessibleTask accessible = requireAccessibleTask(agentTaskId);
    accessService.requireCancelAccess(accessible.task());
    LocalDateTime now = LocalDateTime.now();
    if (taskMapper.updateProgress(
            agentTaskId,
            accessible.task().getVersion(),
            progressPercent,
            trimToNull(currentPhase),
            now)
        == 0) {
      LOGGER.warn(
          "Agent task progress optimistic lock conflict, taskId={}, version={}",
          agentTaskId,
          accessible.task().getVersion());
      throw new ResourceConflictException("Agent 任务已被其他请求更新，请刷新后重试");
    }
    accessible.task().setProgressPercent(progressPercent);
    accessible.task().setCurrentPhase(trimToNull(currentPhase));
    accessible.task().setUpdateTime(now);
    accessible.task().setVersion(accessible.task().getVersion() + 1);
    return toSummary(accessible.task(), accessible.collectionTask().getTaskName());
  }

  @Override
  @Transactional
  public AgentTaskSummaryVO transition(Long agentTaskId, AgentTaskStatus targetStatus) {
    AccessibleTask accessible = requireAccessibleTask(agentTaskId);
    accessService.requireCancelAccess(accessible.task());
    stateMachine.transition(accessible.task(), targetStatus);
    return toSummary(accessible.task(), accessible.collectionTask().getTaskName());
  }

  private AccessibleTask requireAccessibleTask(Long agentTaskId) {
    if (agentTaskId == null || agentTaskId <= 0) {
      throw new BusinessException("Agent 任务 ID 无效");
    }
    AgentTaskEntity task = taskMapper.selectById(agentTaskId);
    if (task == null) {
      throw new ResourceNotFoundException("Agent 任务不存在");
    }
    HerbCollectionTaskEntity collectionTask =
        collectionTaskMapper.selectById(task.getCollectionTaskId());
    if (collectionTask == null) {
      throw new ResourceNotFoundException("Agent 目标采集任务不存在");
    }
    accessService.requireViewAccess(task, collectionTask);
    return new AccessibleTask(task, collectionTask);
  }

  private void validateCreateRequest(AgentTaskCreateRequest request) {
    if (request == null) {
      throw new BusinessException("请求参数不能为空");
    }
    String goalType = trimToNull(request.getGoalType());
    if (goalType == null || !AgentDefinitions.GOAL_TYPES.contains(goalType)) {
      throw new BusinessException("Agent 目标类型无效");
    }
    if (!StringUtils.hasText(request.getGoalText())) {
      throw new BusinessException("目标描述不能为空");
    }
    if (!AgentDefinitions.DEFAULT_TARGET_TYPE.equals(trimToNull(request.getTargetType()))) {
      throw new BusinessException("第一阶段只支持 COLLECTION_TASK 业务目标");
    }
    if (request.getCollectionTaskId() == null || request.getCollectionTaskId() <= 0) {
      throw new BusinessException("采集任务 ID 无效");
    }
    if (request.getTargetId() != null
        && !request.getCollectionTaskId().equals(request.getTargetId())) {
      throw new BusinessException("业务目标 ID 必须与采集任务 ID 一致");
    }
  }

  private String validateSession(String sessionId, Long userId) {
    String normalized = trimToNull(sessionId);
    if (normalized != null && chatSessionMapper.selectBySessionId(normalized, userId) == null) {
      throw new BusinessException("AI 会话不存在或无权关联");
    }
    return normalized;
  }

  private String pageContextJson(String pageContext) {
    String normalized = trimToNull(pageContext);
    if (normalized == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(Map.of("pageContext", normalized));
    } catch (JsonProcessingException exception) {
      throw new BusinessException("页面上下文序列化失败");
    }
  }

  private AgentStepEntity initialStep(Long agentTaskId, LocalDateTime now) {
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(agentTaskId);
    step.setStepNo(1);
    step.setStepType(AgentDefinitions.INITIAL_STEP_TYPE);
    step.setStepName(AgentDefinitions.INITIAL_STEP_NAME);
    step.setDescription("装载采集任务与数字生命档案基础上下文");
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    return step;
  }

  private void normalizeQuery(AgentTaskQueryRequest query) {
    if (query.getPage() == null || query.getPage() < 1) {
      query.setPage(1);
    }
    if (query.getSize() == null || query.getSize() < 1) {
      query.setSize(10);
    } else if (query.getSize() > 200) {
      query.setSize(200);
    }
    query.setStatus(trimToNull(query.getStatus()));
    query.setGoalType(trimToNull(query.getGoalType()));
    if (query.getStatus() != null) {
      AgentTaskStatus.fromCode(query.getStatus());
    }
    if (query.getGoalType() != null && !AgentDefinitions.GOAL_TYPES.contains(query.getGoalType())) {
      throw new BusinessException("Agent 目标类型无效");
    }
  }

  private AgentTaskDetailVO toDetail(
      AgentTaskEntity task, HerbCollectionTaskEntity collectionTask) {
    AgentTaskDetailVO detail = new AgentTaskDetailVO();
    fillSummary(detail, task, collectionTask.getTaskName());
    detail.setResultSummary(task.getResultSummary());
    detail.setErrorCode(task.getErrorCode());
    detail.setErrorMessage(task.getErrorMessage());
    detail.setStartTime(task.getStartTime());
    detail.setFinishTime(task.getFinishTime());
    detail.setCancelTime(task.getCancelTime());
    detail.setSteps(stepMapper.selectByTaskId(task.getId()).stream().map(this::toStepVO).toList());
    detail.setFindings(
        findingMapper.selectByTaskId(task.getId()).stream().map(this::toFindingVO).toList());
    detail.setPendingActions(
        actionMapper.selectByTaskId(task.getId()).stream()
            .filter(action -> PENDING_ACTION_STATUSES.contains(action.getStatus()))
            .map(this::toActionVO)
            .toList());
    return detail;
  }

  private AgentTaskSummaryVO toSummary(AgentTaskEntity task, String targetName) {
    AgentTaskSummaryVO summary = new AgentTaskSummaryVO();
    fillSummary(summary, task, targetName);
    return summary;
  }

  private void fillSummary(AgentTaskSummaryVO summary, AgentTaskEntity task, String targetName) {
    AgentTaskStatus status = AgentTaskStatus.fromCode(task.getStatus());
    summary.setId(task.getId());
    summary.setTaskNo(task.getTaskNo());
    summary.setGoalType(task.getGoalType());
    summary.setGoalText(task.getGoalText());
    summary.setStatus(status.getCode());
    summary.setStatusLabel(status.getLabel());
    summary.setCurrentPhase(task.getCurrentPhase());
    summary.setProgressPercent(task.getProgressPercent());
    summary.setTarget(new AgentTargetVO(task.getTargetType(), task.getTargetId(), targetName));
    summary.setCreateTime(task.getCreateTime());
    summary.setUpdateTime(task.getUpdateTime());
  }

  private AgentStepVO toStepVO(AgentStepEntity step) {
    AgentStepVO vo = new AgentStepVO();
    vo.setId(step.getId());
    vo.setStepNo(step.getStepNo());
    vo.setStepType(step.getStepType());
    vo.setStepName(step.getStepName());
    vo.setDescription(step.getDescription());
    vo.setStatus(step.getStatus());
    vo.setStatusLabel(stepStatusLabel(step.getStatus()));
    vo.setOutputSummary(step.getOutputSummary());
    vo.setErrorCode(step.getErrorCode());
    vo.setErrorMessage(step.getErrorMessage());
    vo.setRetryCount(step.getRetryCount());
    vo.setMaxRetryCount(step.getMaxRetryCount());
    vo.setStartTime(step.getStartTime());
    vo.setFinishTime(step.getFinishTime());
    vo.setCreateTime(step.getCreateTime());
    return vo;
  }

  private AgentFindingVO toFindingVO(AgentFindingEntity finding) {
    AgentFindingVO vo = new AgentFindingVO();
    vo.setId(finding.getId());
    vo.setStepId(finding.getStepId());
    vo.setFindingType(finding.getFindingType());
    vo.setSeverity(finding.getSeverity());
    vo.setTargetType(finding.getTargetType());
    vo.setTargetId(finding.getTargetId());
    vo.setTitle(finding.getTitle());
    vo.setDescription(finding.getDescription());
    vo.setSuggestion(finding.getSuggestion());
    vo.setStatus(finding.getStatus());
    vo.setResolvedTime(finding.getResolvedTime());
    vo.setCreateTime(finding.getCreateTime());
    return vo;
  }

  private AgentActionVO toActionVO(AgentActionEntity action) {
    AgentActionVO vo = new AgentActionVO();
    vo.setId(action.getId());
    vo.setStepId(action.getStepId());
    vo.setActionType(action.getActionType());
    vo.setTargetType(action.getTargetType());
    vo.setTargetId(action.getTargetId());
    vo.setActionName(action.getActionName());
    vo.setActionDescription(action.getActionDescription());
    vo.setRiskLevel(action.getRiskLevel());
    vo.setNeedConfirm(Integer.valueOf(1).equals(action.getNeedConfirm()));
    vo.setStatus(action.getStatus());
    vo.setRequestedTime(action.getRequestedTime());
    vo.setConfirmedBy(action.getConfirmedBy());
    vo.setConfirmedTime(action.getConfirmedTime());
    vo.setRejectedTime(action.getRejectedTime());
    vo.setExecutedTime(action.getExecutedTime());
    vo.setResultSummary(action.getResultSummary());
    vo.setErrorMessage(action.getErrorMessage());
    return vo;
  }

  private String resolveTargetName(Long collectionTaskId) {
    HerbCollectionTaskEntity target = collectionTaskMapper.selectById(collectionTaskId);
    return target == null ? "采集任务已不可用" : target.getTaskName();
  }

  private String stepStatusLabel(String status) {
    return switch (status) {
      case "PENDING" -> "待执行";
      case "RUNNING" -> "执行中";
      case "WAITING" -> "等待中";
      case "SUCCEEDED" -> "已成功";
      case "FAILED" -> "已失败";
      case "SKIPPED" -> "已跳过";
      case "CANCELLED" -> "已取消";
      default -> status;
    };
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private record AccessibleTask(AgentTaskEntity task, HerbCollectionTaskEntity collectionTask) {}
}
