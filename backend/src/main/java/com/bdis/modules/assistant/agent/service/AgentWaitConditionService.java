package com.bdis.modules.assistant.agent.service;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.ConditionDefinition;
import com.bdis.modules.assistant.agent.dto.AgentFieldDataModels.Snapshot;
import com.bdis.modules.assistant.agent.entity.AgentActionEntity;
import com.bdis.modules.assistant.agent.entity.AgentStepEntity;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentActionMapper;
import com.bdis.modules.assistant.agent.mapper.AgentStepMapper;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentWaitConditionService {

  private final AgentWaitConditionMapper waitConditionMapper;
  private final AgentTaskMapper taskMapper;
  private final AgentStepMapper stepMapper;
  private final AgentActionMapper actionMapper;
  private final AgentFieldDataConditionEvaluator evaluator;
  private final AgentTaskStateMachine stateMachine;
  private final ObjectMapper objectMapper;
  private final ApplicationEventPublisher eventPublisher;

  public AgentWaitConditionService(
      AgentWaitConditionMapper waitConditionMapper,
      AgentTaskMapper taskMapper,
      AgentStepMapper stepMapper,
      AgentActionMapper actionMapper,
      AgentFieldDataConditionEvaluator evaluator,
      AgentTaskStateMachine stateMachine,
      ObjectMapper objectMapper,
      ApplicationEventPublisher eventPublisher) {
    this.waitConditionMapper = waitConditionMapper;
    this.taskMapper = taskMapper;
    this.stepMapper = stepMapper;
    this.actionMapper = actionMapper;
    this.evaluator = evaluator;
    this.stateMachine = stateMachine;
    this.objectMapper = objectMapper;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public void checkByFollowUpTaskId(Long followUpTaskId) {
    waitConditionMapper.selectActiveByFollowUpTaskId(followUpTaskId).forEach(this::check);
  }

  @Transactional
  public void check(Long conditionId) {
    AgentWaitConditionEntity condition = waitConditionMapper.selectById(conditionId);
    if (condition != null) {
      check(condition);
    }
  }

  private void check(AgentWaitConditionEntity condition) {
    if ("SATISFIED".equals(condition.getStatus())) {
      waitConditionMapper.incrementTerminalCheckCount(condition.getId(), LocalDateTime.now());
      recover(condition);
      return;
    }
    if (!List.of("WAITING", "PARTIALLY_SATISFIED").contains(condition.getStatus())) {
      waitConditionMapper.incrementTerminalCheckCount(condition.getId(), LocalDateTime.now());
      return;
    }
    ConditionDefinition definition = read(condition.getConditionJson(), ConditionDefinition.class);
    Snapshot snapshot = evaluator.evaluate(condition.getFollowUpTaskId(), definition);
    LocalDateTime now = LocalDateTime.now();
    String status =
        snapshot.satisfied()
            ? "SATISFIED"
            : condition.getDeadline() != null && !condition.getDeadline().isAfter(now)
                ? "EXPIRED"
                : snapshot.completedRequirements().isEmpty() ? "WAITING" : "PARTIALLY_SATISFIED";
    int updated =
        waitConditionMapper.updateCheck(
            condition.getId(),
            condition.getVersion(),
            status,
            write(snapshot),
            now,
            "SATISFIED".equals(status) ? now : null);
    if (updated == 0) {
      return;
    }
    condition.setStatus(status);
    condition.setCurrentSnapshotJson(write(snapshot));
    condition.setVersion(condition.getVersion() + 1);
    if ("SATISFIED".equals(status)) {
      recover(condition);
    } else if ("EXPIRED".equals(status)) {
      expire(condition, snapshot);
    }
  }

  private void recover(AgentWaitConditionEntity condition) {
    AgentTaskEntity task = taskMapper.selectById(condition.getAgentTaskId());
    if (task == null || !AgentTaskStatus.WAITING_FIELD_DATA.getCode().equals(task.getStatus())) {
      return;
    }
    AgentTaskEntity reanalyzing =
        stateMachine.transition(task, AgentTaskStatus.REANALYZING, "复测现场数据已满足等待条件");
    LocalDateTime now = LocalDateTime.now();
    if (stepMapper.completeWaiting(
            condition.getAgentStepId(), "复测现场数据已回传并满足组合条件", condition.getCurrentSnapshotJson(), now)
        == 0) {
      throw new ResourceConflictException("等待现场数据步骤已被其他线程处理");
    }
    createReanalyzeStep(task.getId(), now);
    if (taskMapper.updateProgress(
            task.getId(), reanalyzing.getVersion(), 75, "FOLLOW_UP_DATA_RECEIVED", now)
        == 0) {
      throw new ResourceConflictException("Agent 恢复进度更新冲突");
    }
    eventPublisher.publishEvent(new AgentWorkflowResumeRequestedEvent(task.getId()));
  }

  private void expire(AgentWaitConditionEntity condition, Snapshot snapshot) {
    AgentTaskEntity task = taskMapper.selectById(condition.getAgentTaskId());
    if (task == null || !AgentTaskStatus.WAITING_FIELD_DATA.getCode().equals(task.getStatus())) {
      return;
    }
    stateMachine.transition(task, AgentTaskStatus.WAITING_CONFIRMATION, "现场数据等待已过期");
    LocalDateTime now = LocalDateTime.now();
    AgentActionEntity action = new AgentActionEntity();
    action.setAgentTaskId(task.getId());
    action.setStepId(condition.getAgentStepId());
    action.setActionType("RESOLVE_EXPIRED_FIELD_DATA_WAIT");
    action.setTargetType("AGENT_WAIT_CONDITION");
    action.setTargetId(condition.getId());
    action.setActionName("处理已过期的现场数据等待");
    action.setActionDescription("可选择延长等待、重新生成方案或结束本次研究任务。");
    action.setPayloadJson(write(snapshot));
    action.setRiskLevel("MEDIUM");
    action.setNeedConfirm(1);
    action.setStatus("WAITING_CONFIRMATION");
    action.setRequestedTime(now);
    action.setVersion(0);
    action.setCreateTime(now);
    action.setUpdateTime(now);
    actionMapper.insert(action);
  }

  private void createReanalyzeStep(Long taskId, LocalDateTime now) {
    int next = value(stepMapper.selectMaxStepNo(taskId)) + 1;
    AgentStepEntity step = new AgentStepEntity();
    step.setAgentTaskId(taskId);
    step.setStepNo(next);
    step.setStepType("REANALYZE");
    step.setStepName("重新分析补采数据");
    step.setDescription("任务 7 仅创建待执行步骤，不在事件线程执行重新分析");
    step.setStatus("PENDING");
    step.setRetryCount(0);
    step.setMaxRetryCount(0);
    step.setCreateTime(now);
    step.setUpdateTime(now);
    stepMapper.insertIfAbsent(step);
  }

  private <T> T read(String json, Class<T> type) {
    try {
      return objectMapper.readValue(json, type);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("Agent 等待条件格式无效");
    }
  }

  private String write(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new BusinessException("Agent 等待快照序列化失败");
    }
  }

  private int value(Integer value) {
    return value == null ? 0 : value;
  }

  public record AgentWorkflowResumeRequestedEvent(Long agentTaskId) {}
}
