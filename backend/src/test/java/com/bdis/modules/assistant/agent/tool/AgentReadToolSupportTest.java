package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.agent.service.HerbDigitalTwinAgentTaskService;
import com.bdis.modules.assistant.agent.vo.AgentTargetVO;
import com.bdis.modules.assistant.agent.vo.AgentTaskDetailVO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

class AgentReadToolSupportTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void collectorCanUseContextOnlyForAgentTargetTask() {
        authenticate(7L, Set.of("COLLECTOR"));
        HerbDigitalTwinAgentTaskService taskService = mock(HerbDigitalTwinAgentTaskService.class);
        when(taskService.getDetail(1L)).thenReturn(agentTask(12L));
        AgentReadToolSupport support = new AgentReadToolSupport(taskService);
        AgentToolExecutionContext context = context(12L);

        assertThatCode(() -> support.requireTaskContext(context, 12L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> support.requireTaskContext(context, 13L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("不属于当前 Agent 任务");
    }

    @Test
    void administratorCanReadMatchingAgentTarget() {
        authenticate(1L, Set.of("ADMIN"));
        HerbDigitalTwinAgentTaskService taskService = mock(HerbDigitalTwinAgentTaskService.class);
        when(taskService.getDetail(9L)).thenReturn(agentTask(99L));
        AgentReadToolSupport support = new AgentReadToolSupport(taskService);
        AgentToolExecutionContext context =
                AgentToolExecutionContext.fromCurrentUser(
                        new AgentToolExecutionContext.Scope(
                                null, 9L, null, null, 99L, null, null, null, "admin-request"));

        assertThatCode(() -> support.requireTaskContext(context, 99L)).doesNotThrowAnyException();
    }

    @Test
    void rejectsContextAfterCurrentUserChanges() {
        authenticate(7L, Set.of("COLLECTOR"));
        HerbDigitalTwinAgentTaskService taskService = mock(HerbDigitalTwinAgentTaskService.class);
        AgentReadToolSupport support = new AgentReadToolSupport(taskService);
        AgentToolExecutionContext context = context(12L);
        authenticate(8L, Set.of("COLLECTOR"));

        assertThatThrownBy(() -> support.requireTaskContext(context, 12L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("上下文已失效");
    }

    @Test
    void mapsEnglishStatusToChineseWithoutInventingNullValue() {
        AgentReadToolSupport support =
                new AgentReadToolSupport(mock(HerbDigitalTwinAgentTaskService.class));

        assertThat(support.status("approved").label()).isEqualTo("已通过");
        assertThat(support.status("waiting_custom").label()).isEqualTo("waiting_custom");
        assertThat(support.status(null).code()).isNull();
        assertThat(support.status(null).label()).isNull();
    }

    private AgentTaskDetailVO agentTask(Long targetId) {
        AgentTaskDetailVO detail = new AgentTaskDetailVO();
        detail.setTarget(new AgentTargetVO("COLLECTION_TASK", targetId, "测试任务"));
        return detail;
    }

    private AgentToolExecutionContext context(Long collectionTaskId) {
        return AgentToolExecutionContext.fromCurrentUser(
                new AgentToolExecutionContext.Scope(
                        null, 1L, null, null, collectionTaskId, null, null, null, "request-1"));
    }

    private void authenticate(Long userId, Set<String> roles) {
        CurrentUser user =
                new CurrentUser(userId, "user" + userId, "用户", 1L, 1L, roles, Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
