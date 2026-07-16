package com.bdis.modules.assistant.agent.tool;

import com.bdis.common.exception.BusinessException;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentToolDefinition> definitions;

    public AgentToolRegistry(List<AgentBusinessTool> tools) {
        Map<String, AgentToolDefinition> registered = new LinkedHashMap<>();
        for (AgentBusinessTool tool : tools) {
            for (AgentToolDefinition definition : tool.definitions()) {
                AgentToolDefinition previous =
                        registered.putIfAbsent(definition.toolName(), definition);
                if (previous != null) {
                    throw new IllegalStateException("Agent 工具名称重复注册: " + definition.toolName());
                }
            }
        }
        this.definitions = Map.copyOf(registered);
    }

    public AgentToolDefinition requireDefinition(String toolName) {
        AgentToolDefinition definition = definitions.get(toolName);
        if (definition == null) {
            throw new BusinessException("Agent 工具未注册: " + toolName);
        }
        return definition;
    }

    public List<AgentToolDefinition> listReadOnlyDefinitions() {
        return definitions.values().stream()
                .filter(
                        definition ->
                                definition.toolMode() == AgentToolDefinition.ToolMode.READ_ONLY)
                .toList();
    }
}
