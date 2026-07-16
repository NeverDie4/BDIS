package com.bdis.modules.assistant.agent.event;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class AgentFieldDataEventPublisher {

  private final ApplicationEventPublisher eventPublisher;

  public AgentFieldDataEventPublisher(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  public void publish(
      AgentFieldDataChangedEvent.Type type,
      Long taskId,
      Long batchId,
      Long imageId,
      Long growthRecordId) {
    if (taskId != null) {
      eventPublisher.publishEvent(
          new AgentFieldDataChangedEvent(type, taskId, batchId, imageId, growthRecordId));
    }
  }
}
