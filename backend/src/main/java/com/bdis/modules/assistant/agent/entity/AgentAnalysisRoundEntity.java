package com.bdis.modules.assistant.agent.entity;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgentAnalysisRoundEntity {

  private Long id;
  private Long agentTaskId;
  private Integer roundNo;
  private String roundType;
  private Long sourceCollectionTaskId;
  private Long followUpCollectionTaskId;
  private String baselineSnapshot;
  private String baselineHash;
  private String currentSnapshot;
  private String currentHash;
  private String changeSummary;
  private String conclusion;
  private String outcome;
  private String status;
  private LocalDateTime startTime;
  private LocalDateTime finishTime;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
