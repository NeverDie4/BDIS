package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentActionRejectRequest;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentBusinessLinkEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentBusinessLinkMapper;
import com.bdis.modules.assistant.agent.mapper.AgentFieldDataMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.bdis.modules.assistant.agent.vo.AgentActionExecutionVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveResultVO;
import com.bdis.modules.assistant.agent.vo.AgentDigitalArchiveStatusVO;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.GrowthRecordService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class AgentDigitalArchivePublicationService {

  private static final String RELATION_TYPE = "PUBLIC_DIGITAL_LIFE_ARCHIVE";

  private final AgentActionMapper actionMapper;
  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentBusinessLinkMapper businessLinkMapper;
  private final AgentFieldDataMapper fieldDataMapper;
  private final HerbCollectionTaskMapper collectionTaskMapper;
  private final HerbDigitalTwinAgentTaskService agentTaskService;
  private final AgentDigitalArchiveService digitalArchiveService;
  private final GrowthRecordService growthRecordService;
  private final HerbDigitalLifeArchiveService archiveService;
  private final DigitalLifeIntegrityService integrityService;
  private final AgentTaskStateMachine stateMachine;
  private final ObjectMapper objectMapper;
  private final TransactionTemplate transaction;
  private final TransactionTemplate failureTransaction;
  private final ThreadLocal<Boolean> executionStarted = ThreadLocal.withInitial(() -> false);

  public AgentDigitalArchivePublicationService(
      AgentActionMapper actionMapper,
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentBusinessLinkMapper businessLinkMapper,
      AgentFieldDataMapper fieldDataMapper,
      HerbCollectionTaskMapper collectionTaskMapper,
      HerbDigitalTwinAgentTaskService agentTaskService,
      AgentDigitalArchiveService digitalArchiveService,
      GrowthRecordService growthRecordService,
      HerbDigitalLifeArchiveService archiveService,
      DigitalLifeIntegrityService integrityService,
      AgentTaskStateMachine stateMachine,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager) {
    this.actionMapper = actionMapper;
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.businessLinkMapper = businessLinkMapper;
    this.fieldDataMapper = fieldDataMapper;
    this.collectionTaskMapper = collectionTaskMapper;
    this.agentTaskService = agentTaskService;
    this.digitalArchiveService = digitalArchiveService;
    this.growthRecordService = growthRecordService;
    this.archiveService = archiveService;
    this.integrityService = integrityService;
    this.stateMachine = stateMachine;
    this.objectMapper = objectMapper;
    this.transaction = new TransactionTemplate(transactionManager);
    this.failureTransaction = new TransactionTemplate(transactionManager);
    this.failureTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  public AgentActionExecutionVO confirm(Long actionId) {
    AgentBusinessLinkEntity existing =
        businessLinkMapper.selectByAction(actionId, RELATION_TYPE);
    if (existing != null) return existingResult(actionId, existing);
    try {
      return transaction.execute(status -> publish(actionId));
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
    return transaction.execute(status -> completeInternalArchive(actionId, request.reason()));
  }

  private AgentActionExecutionVO publish(Long actionId) {
    AgentBusinessLinkEntity existing =
        businessLinkMapper.selectByAction(actionId, RELATION_TYPE);
    if (existing != null) return existingResult(actionId, existing);
    AgentActionEntity action = requireAction(actionId);
    AgentTaskEntity task = requireTask(action);
    AgentDigitalArchiveStatusVO status = digitalArchiveService.status(task.getId());
    if (!status.prerequisitesSatisfied()) {
      throw new BusinessException("数字生命档案前置条件已发生变化：" + status.message());
    }
    if (!status.integrityVerified()) {
      throw new BusinessException("数字生命档案哈希校验未通过，不能开启公开访问");
    }
    LocalDateTime now = LocalDateTime.now();
    if (actionMapper.confirm(actionId, action.getVersion(), SecurityUtils.currentUser().getUserId(), now) == 0
        || actionMapper.markExecuting(actionId, action.getVersion() + 1, now) == 0) {
      throw new ResourceConflictException("公开动作已被处理或状态已变化");
    }
    executionStarted.set(true);

    List<GrowthRecordEntity> records = fieldDataMapper.selectGrowthRecords(task.getCollectionTaskId());
    if (records.size() < 2 || records.stream().anyMatch(record -> !"approved".equals(record.getReviewStatus()))) {
      throw new BusinessException("至少需要两条审核通过的生长记录才能公开任务级档案");
    }
    for (GrowthRecordEntity record : records) {
      growthRecordService.generateTraceQrCode(record.getId());
      growthRecordService.enablePublicTrace(record.getId());
    }
    HerbCollectionTaskEntity collectionTask = collectionTaskMapper.selectById(task.getCollectionTaskId());
    if (collectionTask == null
        || !Integer.valueOf(1).equals(collectionTask.getPublicVisible())
        || collectionTask.getTraceCode() == null) {
      throw new BusinessException("任务级公开档案未正确开启");
    }
    archiveService.publicArchive(collectionTask.getTraceCode());
    integrityService.generate(collectionTask.getId());
    DigitalLifeIntegrityVO publicIntegrity = integrityService.verifyPublic(collectionTask.getTraceCode());
    if (!publicIntegrity.verified()) {
      throw new BusinessException("公开档案完整性复核失败：" + publicIntegrity.message());
    }

    AgentStepEntity qrStep = stepMapper.selectByTaskIdAndType(task.getId(), "GENERATE_TRACE_QR");
    succeed(qrStep, "任务级公开二维码已生成", null);
    AgentStepEntity waitStep = stepMapper.selectByTaskIdAndType(task.getId(), "WAIT_PUBLIC_CONFIRMATION");
    if (waitStep != null && "WAITING".equals(waitStep.getStatus())) {
      stepMapper.completeWaiting(waitStep.getId(), "用户已确认开启公开档案", null, now);
    }
    AgentDigitalArchiveResultVO result = digitalArchiveService.result(task.getId());
    persistLink(task, action, collectionTask);
    if (actionMapper.markSucceeded(actionId, action.getVersion() + 2, "任务级数字生命档案已公开", now) == 0) {
      throw new ResourceConflictException("公开动作成功状态更新冲突");
    }
    completeReport(task.getId(), result, "可信数字生命档案已生成并公开");
    completeTask(task, "DIGITAL_ARCHIVE_COMPLETED", "可信数字生命档案已生成并通过公开访问验证");
    return new AgentActionExecutionVO(
        actionId, "SUCCEEDED", null, collectionTask.getId(), collectionTask.getTaskCode(),
        collectionTask.getTaskStatus(), "任务级数字生命档案已公开，可通过二维码访问。");
  }

  private AgentActionExecutionVO completeInternalArchive(Long actionId, String reason) {
    AgentActionEntity action = requireAction(actionId);
    AgentTaskEntity task = requireTask(action);
    LocalDateTime now = LocalDateTime.now();
    if (actionMapper.reject(actionId, action.getVersion(), "用户选择不公开：" + reason, now) == 0) {
      throw new ResourceConflictException("公开动作已被处理或状态已变化");
    }
    AgentStepEntity qrStep = stepMapper.selectByTaskIdAndType(task.getId(), "GENERATE_TRACE_QR");
    if (qrStep != null && "PENDING".equals(qrStep.getStatus())) {
      stepMapper.markSkipped(qrStep.getId(), "用户选择不生成公开二维码", now);
    }
    AgentStepEntity waitStep = stepMapper.selectByTaskIdAndType(task.getId(), "WAIT_PUBLIC_CONFIRMATION");
    if (waitStep != null && "WAITING".equals(waitStep.getStatus())) {
      stepMapper.completeWaiting(waitStep.getId(), "用户选择保留内部档案", null, now);
    }
    AgentDigitalArchiveResultVO result = digitalArchiveService.result(task.getId());
    completeReport(task.getId(), result, "内部数字生命档案已完成，尚未公开");
    completeTask(task, "INTERNAL_ARCHIVE_COMPLETED", "内部数字生命档案已完成，用户选择暂不公开");
    return new AgentActionExecutionVO(
        actionId, "REJECTED", null, task.getCollectionTaskId(), null, null,
        "内部数字生命档案已完成，未开启公开访问。");
  }

  private void completeTask(AgentTaskEntity task, String phase, String summary) {
    AgentTaskEntity running = stateMachine.transition(task, AgentTaskStatus.RUNNING);
    agentTaskService.updateProgress(task.getId(), 100, phase);
    AgentTaskEntity refreshed = taskMapper.selectById(task.getId());
    stateMachine.transition(refreshed == null ? running : refreshed, AgentTaskStatus.COMPLETED, summary);
  }

  private void completeReport(Long taskId, AgentDigitalArchiveResultVO result, String summary) {
    AgentStepEntity step = stepMapper.selectByTaskIdAndType(taskId, "COMPLETE_REPORT");
    if (step == null) {
      LocalDateTime now = LocalDateTime.now();
      step = new AgentStepEntity();
      step.setAgentTaskId(taskId);
      step.setStepNo(value(stepMapper.selectMaxStepNo(taskId)) + 1);
      step.setStepType("COMPLETE_REPORT");
      step.setStepName("生成最终档案报告");
      step.setStatus("PENDING");
      step.setRetryCount(0);
      step.setMaxRetryCount(0);
      step.setCreateTime(now);
      step.setUpdateTime(now);
      stepMapper.insertIfAbsent(step);
      step = stepMapper.selectByTaskIdAndType(taskId, "COMPLETE_REPORT");
    }
    succeed(step, summary, toJson(result));
  }

  private void succeed(AgentStepEntity step, String summary, String json) {
    if (step == null || "SUCCEEDED".equals(step.getStatus())) return;
    LocalDateTime now = LocalDateTime.now();
    if ("PENDING".equals(step.getStatus()) && stepMapper.markRunning(step.getId(), now) == 0) {
      throw new ResourceConflictException("档案步骤已被其他请求推进");
    }
    if (stepMapper.markSucceeded(step.getId(), summary, json, LocalDateTime.now()) == 0) {
      throw new ResourceConflictException("档案步骤完成状态更新冲突");
    }
  }

  private AgentActionEntity requireAction(Long actionId) {
    AgentActionEntity action = actionMapper.selectById(actionId);
    if (action == null || !AgentDigitalArchiveService.ENABLE_PUBLIC_TRACE.equals(action.getActionType())) {
      throw new BusinessException("公开档案确认动作不存在");
    }
    if (!"WAITING_CONFIRMATION".equals(action.getStatus())) {
      throw new BusinessException("当前公开动作状态不允许确认或拒绝");
    }
    return action;
  }

  private AgentTaskEntity requireTask(AgentActionEntity action) {
    if (SecurityUtils.currentUser().getRoleCodes().stream()
        .noneMatch(role -> Set.of("ADMIN", "TEACHER", "REVIEWER").contains(role))) {
      throw new BusinessException("仅管理员、教师或审核员可处理公开档案动作");
    }
    agentTaskService.getDetail(action.getAgentTaskId());
    AgentTaskEntity task = taskMapper.selectById(action.getAgentTaskId());
    if (task == null || !AgentTaskStatus.WAITING_CONFIRMATION.getCode().equals(task.getStatus())) {
      throw new BusinessException("Agent 任务当前不在等待公开确认状态");
    }
    return task;
  }

  private void persistLink(
      AgentTaskEntity task, AgentActionEntity action, HerbCollectionTaskEntity collectionTask) {
    AgentBusinessLinkEntity link = new AgentBusinessLinkEntity();
    link.setAgentTaskId(task.getId());
    link.setAgentActionId(action.getId());
    link.setRelationType(RELATION_TYPE);
    link.setBusinessType("DIGITAL_LIFE_ARCHIVE");
    link.setBusinessId(collectionTask.getId());
    link.setBusinessNo(collectionTask.getTraceCode());
    link.setCreateTime(LocalDateTime.now());
    businessLinkMapper.insert(link);
  }

  private AgentActionExecutionVO existingResult(Long actionId, AgentBusinessLinkEntity link) {
    HerbCollectionTaskEntity task = collectionTaskMapper.selectById(link.getBusinessId());
    return new AgentActionExecutionVO(
        actionId, "SUCCEEDED", null, link.getBusinessId(),
        task == null ? null : task.getTaskCode(), task == null ? null : task.getTaskStatus(),
        "任务级数字生命档案已公开，本次请求复用已有结果。");
  }

  private void recordFailure(Long actionId, RuntimeException exception) {
    AgentActionEntity action = actionMapper.selectById(actionId);
    if (action != null) {
      actionMapper.markFailed(
          actionId, action.getVersion(), truncate(exception.getMessage(), 1000), LocalDateTime.now());
    }
  }

  private String toJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("数字生命档案结果序列化失败");
    }
  }

  private String truncate(String value, int max) {
    if (value == null) return "未知错误";
    return value.length() <= max ? value : value.substring(0, max);
  }

  private int value(Integer value) { return value == null ? 0 : value; }
}
