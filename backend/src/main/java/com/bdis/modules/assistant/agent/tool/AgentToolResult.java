package com.bdis.modules.assistant.agent.tool;

import java.time.LocalDateTime;
import java.util.List;

public record AgentToolResult<T>(
        String toolName,
        Boolean success,
        String summary,
        T data,
        List<String> warnings,
        LocalDateTime executedAt,
        Long timeCostMs) {

    public AgentToolResult {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
