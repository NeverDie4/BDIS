package com.bdis.modules.assistant.agent.vo;

public record AgentDigitalArchiveStatusVO(
    Long agentTaskId,
    String status,
    String statusLabel,
    int completenessScore,
    int requiredCompletenessScore,
    boolean prerequisitesSatisfied,
    boolean integrityVerified,
    boolean waitingPublicConfirmation,
    Long pendingActionId,
    String message) {}
