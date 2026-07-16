package com.bdis.modules.assistant.agent.support;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceConflictException;
import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AgentTaskStateMachine {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentTaskStateMachine.class);
  private static final Map<AgentTaskStatus, Set<AgentTaskStatus>> ALLOWED_TRANSITIONS =
      allowedTransitions();

  private final AgentTaskMapper taskMapper;

  public AgentTaskStateMachine(AgentTaskMapper taskMapper) {
    this.taskMapper = taskMapper;
  }

  @Transactional
  public AgentTaskEntity transition(AgentTaskEntity task, AgentTaskStatus targetStatus) {
    return transition(task, targetStatus, null);
  }

  @Transactional
  public AgentTaskEntity transition(
      AgentTaskEntity task, AgentTaskStatus targetStatus, String resultSummary) {
    if (task == null || task.getId() == null) {
      throw new BusinessException("Agent 任务不存在");
    }
    AgentTaskStatus currentStatus = AgentTaskStatus.fromCode(task.getStatus());
    if (currentStatus.isTerminal()) {
      throw new ResourceConflictException("终态 Agent 任务不能再次执行状态转换");
    }
    if (targetStatus == null
        || !ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
      String targetCode = targetStatus == null ? "null" : targetStatus.getCode();
      throw new BusinessException(
          "非法 Agent 任务状态转换：" + currentStatus.getCode() + " -> " + targetCode);
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startTime = currentStatus == AgentTaskStatus.CREATED ? now : task.getStartTime();
    LocalDateTime finishTime = targetStatus.isTerminal() ? now : null;
    LocalDateTime cancelTime = targetStatus == AgentTaskStatus.CANCELLED ? now : null;
    int affected =
        taskMapper.updateState(
            task.getId(),
            currentStatus.getCode(),
            task.getVersion(),
            targetStatus.getCode(),
            resultSummary,
            startTime,
            finishTime,
            cancelTime,
            now);
    if (affected == 0) {
      LOGGER.warn(
          "Agent task optimistic lock conflict, taskId={}, expectedStatus={}, version={}",
          task.getId(),
          currentStatus.getCode(),
          task.getVersion());
      throw new ResourceConflictException("Agent 任务已被其他请求推进，请刷新后重试");
    }

    task.setStatus(targetStatus.getCode());
    if (resultSummary != null) {
      task.setResultSummary(resultSummary);
    }
    task.setStartTime(startTime);
    task.setFinishTime(finishTime == null ? task.getFinishTime() : finishTime);
    task.setCancelTime(cancelTime == null ? task.getCancelTime() : cancelTime);
    task.setUpdateTime(now);
    task.setVersion(task.getVersion() + 1);
    LOGGER.info(
        "Agent task state changed, taskId={}, from={}, to={}",
        task.getId(),
        currentStatus.getCode(),
        targetStatus.getCode());
    return task;
  }

  public boolean canTransition(AgentTaskStatus source, AgentTaskStatus target) {
    return source != null
        && target != null
        && ALLOWED_TRANSITIONS.getOrDefault(source, Set.of()).contains(target);
  }

  private static Map<AgentTaskStatus, Set<AgentTaskStatus>> allowedTransitions() {
    Map<AgentTaskStatus, Set<AgentTaskStatus>> transitions = new EnumMap<>(AgentTaskStatus.class);
    transitions.put(
        AgentTaskStatus.CREATED, Set.of(AgentTaskStatus.PLANNING, AgentTaskStatus.CANCELLED));
    transitions.put(
        AgentTaskStatus.PLANNING,
        Set.of(AgentTaskStatus.RUNNING, AgentTaskStatus.FAILED, AgentTaskStatus.CANCELLED));
    transitions.put(
        AgentTaskStatus.RUNNING,
        Set.of(
            AgentTaskStatus.WAITING_CONFIRMATION,
            AgentTaskStatus.WAITING_FIELD_DATA,
            AgentTaskStatus.REANALYZING,
            AgentTaskStatus.COMPLETED,
            AgentTaskStatus.FAILED,
            AgentTaskStatus.CANCELLED));
    transitions.put(
        AgentTaskStatus.WAITING_CONFIRMATION,
        Set.of(AgentTaskStatus.RUNNING, AgentTaskStatus.CANCELLED, AgentTaskStatus.FAILED));
    transitions.put(
        AgentTaskStatus.WAITING_FIELD_DATA,
        Set.of(
            AgentTaskStatus.REANALYZING,
            AgentTaskStatus.WAITING_CONFIRMATION,
            AgentTaskStatus.CANCELLED,
            AgentTaskStatus.FAILED));
    transitions.put(
        AgentTaskStatus.REANALYZING,
        Set.of(
            AgentTaskStatus.RUNNING,
            AgentTaskStatus.WAITING_CONFIRMATION,
            AgentTaskStatus.WAITING_FIELD_DATA,
            AgentTaskStatus.COMPLETED,
            AgentTaskStatus.FAILED,
            AgentTaskStatus.CANCELLED));
    return Map.copyOf(transitions);
  }
}
