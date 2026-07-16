package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.entity.AgentToolCallLogEntity;
import com.bdis.modules.assistant.agent.mapper.AgentToolCallLogMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

class AgentToolAuditServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditParticipatesInOuterAgentTransactionWithoutOpeningACompetingConnection()
            throws NoSuchMethodException {
        Transactional annotation =
                AgentToolAuditService.class
                        .getMethod(
                                "record",
                                AgentToolExecutionContext.class,
                                String.class,
                                String.class,
                                AgentToolResult.class,
                                String.class,
                                String.class)
                        .getAnnotation(Transactional.class);

        assertThat(annotation.propagation()).isEqualTo(Propagation.NESTED);
    }

    @Test
    void writesSanitizedSuccessLogWithInputHash() {
        authenticate();
        AgentToolCallLogMapper mapper = mock(AgentToolCallLogMapper.class);
        AgentToolAuditService service =
                new AgentToolAuditService(
                        mapper,
                        new AgentToolLogSanitizer(new ObjectMapper().findAndRegisterModules()));
        AgentToolExecutionContext context = context();
        AgentToolResult<PathOutput> result =
                new AgentToolResult<>(
                        "test.read",
                        true,
                        "读取成功",
                        new PathOutput("C:\\bdis\\storage\\image.jpg"),
                        List.of(),
                        LocalDateTime.now(),
                        12L);

        service.record(context, "test.read", "读取任务 12", result, null, null);

        ArgumentCaptor<AgentToolCallLogEntity> captor =
                ArgumentCaptor.forClass(AgentToolCallLogEntity.class);
        verify(mapper).insert(captor.capture());
        AgentToolCallLogEntity log = captor.getValue();
        assertThat(log.getAgentTaskId()).isEqualTo(1L);
        assertThat(log.getUserId()).isEqualTo(7L);
        assertThat(log.getSuccess()).isEqualTo(1);
        assertThat(log.getInputHash()).hasSize(64);
        assertThat(log.getOutputJson()).doesNotContain("C:\\bdis\\storage");
        assertThat(log.getOutputJson()).contains("已脱敏路径");
    }

    @Test
    void writesFailureCodeAndMessage() {
        authenticate();
        AgentToolCallLogMapper mapper = mock(AgentToolCallLogMapper.class);
        AgentToolAuditService service =
                new AgentToolAuditService(
                        mapper,
                        new AgentToolLogSanitizer(new ObjectMapper().findAndRegisterModules()));
        AgentToolResult<Object> result =
                new AgentToolResult<>(
                        "test.read", false, "无权访问", null, List.of("无权访问"), LocalDateTime.now(), 3L);

        service.record(context(), "test.read", "失败请求", result, "FORBIDDEN", "无权访问");

        ArgumentCaptor<AgentToolCallLogEntity> captor =
                ArgumentCaptor.forClass(AgentToolCallLogEntity.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getSuccess()).isZero();
        assertThat(captor.getValue().getErrorCode()).isEqualTo("FORBIDDEN");
        assertThat(captor.getValue().getErrorMessage()).isEqualTo("无权访问");
    }

    private AgentToolExecutionContext context() {
        return AgentToolExecutionContext.fromCurrentUser(
                new AgentToolExecutionContext.Scope(
                        "session", 1L, 2L, null, 12L, null, null, null, "request"));
    }

    private void authenticate() {
        CurrentUser user =
                new CurrentUser(7L, "tester", "测试用户", 1L, 1L, Set.of("ADMIN"), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private record PathOutput(String imageUrl) {}
}
