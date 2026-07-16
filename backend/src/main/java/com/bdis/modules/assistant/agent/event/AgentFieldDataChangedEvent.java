package com.bdis.modules.assistant.agent.event;

public record AgentFieldDataChangedEvent(
    Type type, Long taskId, Long batchId, Long imageId, Long growthRecordId) {

  public enum Type {
    BATCH_CREATED,
    IMAGE_UPLOADED,
    IMAGE_RECOGNITION_COMPLETED,
    GROWTH_RECORD_CREATED,
    GROWTH_RECORD_UPDATED,
    GROWTH_RECORD_SUBMITTED,
    COLLECTION_TASK_PROGRESS_CHANGED
  }
}
