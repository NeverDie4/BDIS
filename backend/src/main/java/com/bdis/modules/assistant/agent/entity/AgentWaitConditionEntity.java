package com.bdis.modules.assistant.agent.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentWaitConditionEntity {

  private Long id;
  private Long agentTaskId;
  private Long agentStepId;
  private Long collectionPlanId;
  private Long followUpTaskId;
  private String conditionType;
  private String conditionJson;
  private String currentSnapshotJson;
  private String status;
  private LocalDateTime deadline;
  private LocalDateTime lastCheckTime;
  private LocalDateTime satisfiedTime;
  private Integer checkCount;
  private Integer version;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
