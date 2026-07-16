package com.bdis.modules.assistant.agent.tool;

import com.bdis.modules.assistant.agent.entity.AgentToolCallLogEntity;
import com.bdis.modules.assistant.agent.mapper.AgentToolCallLogMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AgentToolAuditService {

    private final AgentToolCallLogMapper logMapper;
    private final AgentToolLogSanitizer sanitizer;

    public AgentToolAuditService(
            AgentToolCallLogMapper logMapper, AgentToolLogSanitizer sanitizer) {
        this.logMapper = logMapper;
        this.sanitizer = sanitizer;
    }

    @Transactional(propagation = Propagation.NESTED)
    public void record(
            AgentToolExecutionContext context,
            String toolName,
            String requestSummary,
            AgentToolResult<?> result,
            String errorCode,
            String errorMessage) {
        if (context == null || context.getAgentTaskId() == null) {
            return;
        }
        AgentToolCallLogEntity log = new AgentToolCallLogEntity();
        log.setAgentTaskId(context.getAgentTaskId());
        log.setStepId(context.getAgentStepId());
        log.setUserId(context.getUserId());
        log.setSessionId(sanitizer.text(context.getSessionId(), 128));
        log.setToolName(sanitizer.text(toolName, 128));
        log.setRequestSummary(sanitizer.text(requestSummary, 1000));
        log.setInputHash(sanitizer.sha256(requestSummary));
        log.setOutputSummary(sanitizer.text(result.summary(), 4000));
        log.setOutputJson(sanitizer.outputJson(result));
        log.setSuccess(Boolean.TRUE.equals(result.success()) ? 1 : 0);
        log.setErrorCode(sanitizer.text(errorCode, 64));
        log.setErrorMessage(sanitizer.text(errorMessage, 1000));
        log.setTimeCostMs(result.timeCostMs());
        log.setCreateTime(result.executedAt() == null ? LocalDateTime.now() : result.executedAt());
        logMapper.insert(log);
    }
}
