package com.bdis.modules.assistant.agent.vo;

import java.time.LocalDateTime;

public record AgentAnalysisRoundVO(
    Long id,
    Integer roundNo,
    String roundType,
    Long sourceCollectionTaskId,
    Long followUpCollectionTaskId,
    String baselineHash,
    String currentHash,
    String conclusion,
    String outcome,
    String status,
    LocalDateTime startTime,
    LocalDateTime finishTime) {}
