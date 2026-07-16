package com.bdis.modules.assistant.agent.event;

import com.bdis.modules.assistant.agent.service.AgentReanalysisFailureRecorder;
import com.bdis.modules.assistant.agent.service.AgentReanalysisService;
import com.bdis.modules.assistant.agent.service.AgentWaitConditionService.AgentWorkflowResumeRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AgentWorkflowResumeDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(AgentWorkflowResumeDispatcher.class);

  private final AgentReanalysisService reanalysisService;
  private final AgentReanalysisFailureRecorder failureRecorder;

  public AgentWorkflowResumeDispatcher(
      AgentReanalysisService reanalysisService, AgentReanalysisFailureRecorder failureRecorder) {
    this.reanalysisService = reanalysisService;
    this.failureRecorder = failureRecorder;
  }

  @Async("agentToolWorkerExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void dispatch(AgentWorkflowResumeRequestedEvent event) {
    try {
      reanalysisService.run(event.agentTaskId());
    } catch (RuntimeException exception) {
      failureRecorder.record(event.agentTaskId(), exception);
      LOGGER.warn(
          "Agent reanalysis dispatch failed, taskId={}: {}",
          event.agentTaskId(),
          exception.getMessage());
    }
  }
}
