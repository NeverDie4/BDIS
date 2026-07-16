package com.bdis.modules.assistant.agent.tool;

import java.util.Set;

public record AgentToolDefinition(
        String toolName,
        String displayName,
        String description,
        ToolMode toolMode,
        Set<String> requiredRoles,
        RiskLevel riskLevel,
        int timeoutSeconds) {

    public AgentToolDefinition {
        requiredRoles = requiredRoles == null ? Set.of() : Set.copyOf(requiredRoles);
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("工具超时时间必须大于 0 秒");
        }
    }

    public enum ToolMode {
        READ_ONLY,
        WRITE,
        EXTERNAL
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}
