package com.bdis.modules.assistant.agent.tool;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.assistant.agent.tool.AgentToolDefinition.RiskLevel;
import com.bdis.modules.assistant.agent.tool.AgentToolDefinition.ToolMode;
import com.bdis.modules.assistant.agent.tool.dto.StatusValue;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Set;

@Component
public class AgentReadToolSupport {

    public static final Set<String> BUSINESS_ROLES =
            Set.of("ADMIN", "TEACHER", "COLLECTOR", "REVIEWER");

    private static final Map<String, String> STATUS_LABELS =
            Map.ofEntries(
                    Map.entry("draft", "草稿"),
                    Map.entry("published", "已发布"),
                    Map.entry("in_progress", "进行中"),
                    Map.entry("collecting", "采集中"),
                    Map.entry("submitted", "待审核"),
                    Map.entry("identifying", "识别中"),
                    Map.entry("reviewing", "复核中"),
                    Map.entry("confirmed", "已确认"),
                    Map.entry("approved", "已通过"),
                    Map.entry("rejected", "已驳回"),
                    Map.entry("archived", "已归档"),
                    Map.entry("completed", "已完成"),
                    Map.entry("cancelled", "已取消"),
                    Map.entry("pending", "待处理"),
                    Map.entry("success", "成功"),
                    Map.entry("failed", "失败"),
                    Map.entry("ready", "已就绪"),
                    Map.entry("not_ready", "未就绪"));

    private final HerbDigitalTwinAgentTaskService agentTaskService;

    public AgentReadToolSupport(HerbDigitalTwinAgentTaskService agentTaskService) {
        this.agentTaskService = agentTaskService;
    }

    public void requireTaskContext(AgentToolExecutionContext context, Long collectionTaskId) {
        if (context == null || context.getAgentTaskId() == null) {
            throw new BusinessException("Agent 工具执行上下文不完整");
        }
        CurrentUser current = SecurityUtils.currentUser();
        if (!current.getUserId().equals(context.getUserId())
                || !current.getRoleCodes().equals(context.getRoles())) {
            throw new ForbiddenException("Agent 工具用户上下文已失效");
        }
        AgentTaskDetailVO agentTask = agentTaskService.getDetail(context.getAgentTaskId());
        Long targetId = agentTask.getTarget() == null ? null : agentTask.getTarget().getId();
        if (collectionTaskId == null || !collectionTaskId.equals(targetId)) {
            throw new ForbiddenException("工具目标不属于当前 Agent 任务");
        }
        if (context.getCollectionTaskId() != null
                && !collectionTaskId.equals(context.getCollectionTaskId())) {
            throw new ForbiddenException("工具目标与执行上下文不一致");
        }
    }

    public StatusValue status(String code) {
        if (!StringUtils.hasText(code)) {
            return new StatusValue(code, null);
        }
        return new StatusValue(code, STATUS_LABELS.getOrDefault(code, code));
    }

    public String browserUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        return url.startsWith("/api/")
                        || url.startsWith("/storage/")
                        || url.startsWith("http://")
                        || url.startsWith("https://")
                ? url
                : null;
    }

    public AgentToolDefinition readDefinition(
            String toolName, String displayName, String description) {
        return new AgentToolDefinition(
                toolName,
                displayName,
                description,
                ToolMode.READ_ONLY,
                BUSINESS_ROLES,
                RiskLevel.LOW,
                15);
    }
}
