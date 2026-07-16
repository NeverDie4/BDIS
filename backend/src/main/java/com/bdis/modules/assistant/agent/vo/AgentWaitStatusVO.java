package com.bdis.modules.assistant.agent.vo;

import java.time.LocalDateTime;
import java.util.List;

public record AgentWaitStatusVO(
    Long followUpTaskId,
    String followUpTaskName,
    String status,
    LocalDateTime deadline,
    List<String> completedRequirements,
    List<String> missingRequirements,
    int progressPercent,
    LocalDateTime lastCheckTime,
    String nextActionHint) {}
