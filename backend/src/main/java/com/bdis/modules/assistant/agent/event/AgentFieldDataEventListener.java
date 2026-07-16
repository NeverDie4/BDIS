package com.bdis.modules.assistant.agent.event;

import com.bdis.modules.assistant.agent.service.AgentWaitConditionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AgentFieldDataEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentFieldDataEventListener.class);

  private final AgentWaitConditionService waitConditionService;

  public AgentFieldDataEventListener(AgentWaitConditionService waitConditionService) {
    this.waitConditionService = waitConditionService;
  }

  @Async("agentToolWorkerExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onFieldDataChanged(AgentFieldDataChangedEvent event) {
    try {
      waitConditionService.checkByFollowUpTaskId(event.taskId());
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Agent field data event check failed, type={}, taskId={}: {}",
          event.type(),
          event.taskId(),
          exception.getMessage());
    }
  }
}
