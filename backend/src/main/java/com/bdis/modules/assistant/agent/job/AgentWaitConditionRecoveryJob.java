package com.bdis.modules.assistant.agent.job;

import com.bdis.modules.assistant.agent.entity.AgentWaitConditionEntity;
import com.bdis.modules.assistant.agent.mapper.AgentWaitConditionMapper;
import com.bdis.modules.assistant.agent.service.AgentWaitConditionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AgentWaitConditionRecoveryJob {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentWaitConditionRecoveryJob.class);

  private final AgentWaitConditionMapper waitConditionMapper;
  private final AgentWaitConditionService waitConditionService;

  @Value("${assistant.agent.wait-check-batch-size:100}")
  private int batchSize;

  public AgentWaitConditionRecoveryJob(
      AgentWaitConditionMapper waitConditionMapper,
      AgentWaitConditionService waitConditionService) {
    this.waitConditionMapper = waitConditionMapper;
    this.waitConditionService = waitConditionService;
  }

  @Scheduled(fixedDelayString = "#{${assistant.agent.wait-check-interval-seconds:300} * 1000}")
  public void recoverWaitingTasks() {
    for (AgentWaitConditionEntity condition :
        waitConditionMapper.selectRecoveryCandidates(Math.max(1, batchSize))) {
      try {
        waitConditionService.check(condition.getId());
      } catch (RuntimeException exception) {
        LOGGER.warn(
            "Agent wait compensation check failed, conditionId={}, taskId={}: {}",
            condition.getId(),
            condition.getAgentTaskId(),
            exception.getMessage());
      }
    }
  }
}
