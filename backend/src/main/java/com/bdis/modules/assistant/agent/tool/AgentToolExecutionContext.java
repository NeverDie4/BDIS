package com.bdis.modules.assistant.agent.tool;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;

import lombok.Getter;

import java.util.Set;

/** Agent 工具的后端内部执行上下文，身份字段只能从当前安全上下文生成。 */
@Getter
public final class AgentToolExecutionContext {

    private final Long userId;
    private final String username;
    private final Set<String> roles;
    private final String sessionId;
    private final Long agentTaskId;
    private final Long agentStepId;
    private final String pageContext;
    private final Long collectionTaskId;
    private final Long batchId;
    private final Long growthRecordId;
    private final Long imageId;
    private final String requestId;

    private AgentToolExecutionContext(CurrentUser currentUser, Scope scope) {
        this.userId = currentUser.getUserId();
        this.username = currentUser.getUsername();
        this.roles = Set.copyOf(currentUser.getRoleCodes());
        this.sessionId = scope.sessionId();
        this.agentTaskId = scope.agentTaskId();
        this.agentStepId = scope.agentStepId();
        this.pageContext = scope.pageContext();
        this.collectionTaskId = scope.collectionTaskId();
        this.batchId = scope.batchId();
        this.growthRecordId = scope.growthRecordId();
        this.imageId = scope.imageId();
        this.requestId = scope.requestId();
    }

    public static AgentToolExecutionContext fromCurrentUser(Scope scope) {
        if (scope == null) {
            throw new IllegalArgumentException("Agent 工具执行范围不能为空");
        }
        return new AgentToolExecutionContext(SecurityUtils.currentUser(), scope);
    }

    /** 仅包含业务定位信息，不包含可由调用方指定的用户身份。 */
    public record Scope(
            String sessionId,
            Long agentTaskId,
            Long agentStepId,
            String pageContext,
            Long collectionTaskId,
            Long batchId,
            Long growthRecordId,
            Long imageId,
            String requestId) {}
}
