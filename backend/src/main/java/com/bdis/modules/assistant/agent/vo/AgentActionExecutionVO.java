package com.bdis.modules.assistant.agent.vo;

public record AgentActionExecutionVO(
    Long actionId,
    String actionStatus,
    Long collectionPlanId,
    Long collectionTaskId,
    String collectionTaskNo,
    String collectionTaskStatus,
    String message) {}
