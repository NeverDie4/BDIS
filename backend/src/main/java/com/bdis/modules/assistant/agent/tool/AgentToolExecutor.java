package com.bdis.modules.assistant.agent.tool;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class AgentToolExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentToolExecutor.class);
    private static final String WRITE_FORBIDDEN = "当前阶段不允许 Agent 执行业务写操作。";

    private final AgentToolRegistry registry;
    private final AgentToolAuditService auditService;
    private final AsyncTaskExecutor taskExecutor;

    public AgentToolExecutor(
            AgentToolRegistry registry,
            AgentToolAuditService auditService,
            @Qualifier("agentToolSecurityExecutor") AsyncTaskExecutor taskExecutor) {
        this.registry = registry;
        this.auditService = auditService;
        this.taskExecutor = taskExecutor;
    }

    public <T> AgentToolResult<T> execute(
            String toolName,
            AgentToolExecutionContext context,
            String requestSummary,
            AgentToolInvocation<T> invocation) {
        long started = System.nanoTime();
        LocalDateTime executedAt = LocalDateTime.now();
        try {
            AgentToolDefinition definition = registry.requireDefinition(toolName);
            if (definition.toolMode() != AgentToolDefinition.ToolMode.READ_ONLY) {
                return failure(
                        context,
                        toolName,
                        requestSummary,
                        WRITE_FORBIDDEN,
                        "WRITE_TOOL_FORBIDDEN",
                        started,
                        executedAt);
            }
            validateContextAndRoles(context, definition);
            Future<T> future = taskExecutor.submit(invocation::execute);
            T data;
            try {
                data = future.get(definition.timeoutSeconds(), TimeUnit.SECONDS);
            } catch (TimeoutException exception) {
                future.cancel(true);
                return failure(
                        context,
                        toolName,
                        requestSummary,
                        "Agent 工具执行超时",
                        "TOOL_TIMEOUT",
                        started,
                        executedAt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return failure(
                        context,
                        toolName,
                        requestSummary,
                        "Agent 工具执行被中断",
                        "TOOL_INTERRUPTED",
                        started,
                        executedAt);
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                return failure(
                        context,
                        toolName,
                        requestSummary,
                        cause.getMessage(),
                        errorCode(cause),
                        started,
                        executedAt);
            }
            long timeCostMs = elapsedMillis(started);
            AgentToolResult<T> result =
                    new AgentToolResult<>(
                            toolName,
                            true,
                            definition.displayName() + "读取成功",
                            data,
                            List.of(),
                            executedAt,
                            timeCostMs);
            auditService.record(context, toolName, requestSummary, result, null, null);
            return result;
        } catch (RuntimeException exception) {
            return failure(
                    context,
                    toolName,
                    requestSummary,
                    exception.getMessage(),
                    errorCode(exception),
                    started,
                    executedAt);
        }
    }

    private void validateContextAndRoles(
            AgentToolExecutionContext context, AgentToolDefinition definition) {
        if (context == null || context.getAgentTaskId() == null || context.getUserId() == null) {
            throw new BusinessException("Agent 工具执行上下文不完整");
        }
        CurrentUser current = SecurityUtils.currentUser();
        if (!current.getUserId().equals(context.getUserId())
                || !current.getRoleCodes().equals(context.getRoles())) {
            throw new BusinessException("Agent 工具用户上下文已失效");
        }
        if (!definition.requiredRoles().isEmpty()
                && context.getRoles().stream().noneMatch(definition.requiredRoles()::contains)) {
            throw new BusinessException("当前角色无权调用该 Agent 工具");
        }
    }

    private <T> AgentToolResult<T> failure(
            AgentToolExecutionContext context,
            String toolName,
            String requestSummary,
            String message,
            String errorCode,
            long started,
            LocalDateTime executedAt) {
        String safeMessage = message == null ? "Agent 工具执行失败" : message;
        AgentToolResult<T> result =
                new AgentToolResult<>(
                        toolName,
                        false,
                        safeMessage,
                        null,
                        List.of(safeMessage),
                        executedAt,
                        elapsedMillis(started));
        auditService.record(context, toolName, requestSummary, result, errorCode, safeMessage);
        LOGGER.warn(
                "Agent tool call failed, agentTaskId={}, toolName={}, errorCode={}",
                context == null ? null : context.getAgentTaskId(),
                toolName,
                errorCode);
        return result;
    }

    private String errorCode(Throwable throwable) {
        if (throwable instanceof BusinessException) {
            return "BUSINESS_ERROR";
        }
        return "TOOL_EXECUTION_FAILED";
    }

    private long elapsedMillis(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    @FunctionalInterface
    public interface AgentToolInvocation<T> {
        T execute();
    }
}
