package com.bdis.modules.assistant.agent.tool;

import java.util.List;

/** 标识一组同领域的 Agent 业务工具操作。 */
public interface AgentBusinessTool {

    List<AgentToolDefinition> definitions();
}
