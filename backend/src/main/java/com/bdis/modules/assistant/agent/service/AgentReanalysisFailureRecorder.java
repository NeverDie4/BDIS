package com.bdis.modules.assistant.agent.service;

import com.bdis.modules.assistant.agent.constant.AgentTaskStatus;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.assistant.agent.mapper.AgentTaskMapper;
import com.bdis.modules.assistant.agent.support.AgentTaskStateMachine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AgentReanalysisFailureRecorder {

  private final AgentTaskMapper taskMapper;
  private final AgentTaskStateMachine stateMachine;

  public AgentReanalysisFailureRecorder(
      AgentTaskMapper taskMapper, AgentTaskStateMachine stateMachine) {
    this.taskMapper = taskMapper;
    this.stateMachine = stateMachine;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(Long agentTaskId, RuntimeException exception) {
    AgentTaskEntity task = taskMapper.selectById(agentTaskId);
    if (task != null && AgentTaskStatus.REANALYZING.getCode().equals(task.getStatus())) {
      String message =
          exception == null || !StringUtils.hasText(exception.getMessage())
              ? "重新分析发生系统错误"
              : exception.getMessage();
      stateMachine.transition(
          task,
          AgentTaskStatus.FAILED,
          "重新分析发生不可恢复的系统错误：" + message.substring(0, Math.min(message.length(), 500)));
    }
  }
}
