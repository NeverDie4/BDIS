package com.bdis.modules.assistant.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.tool.AgentToolDefinition.RiskLevel;
import com.bdis.modules.assistant.agent.tool.AgentToolDefinition.ToolMode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.List;
import java.util.Set;

class AgentToolExecutorTest {

    private ThreadPoolTaskExecutor worker;
    private AgentToolAuditService auditService;

    @BeforeEach
    void setUp() {
        authenticate(Set.of("ADMIN"));
        worker = new ThreadPoolTaskExecutor();
        worker.setCorePoolSize(1);
        worker.setMaxPoolSize(1);
        worker.initialize();
        auditService = mock(AgentToolAuditService.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        worker.shutdown();
    }

    @Test
    void executesReadToolOnWorkerWithSecurityContextAndWritesSuccessLog() {
        AgentToolExecutor executor =
                executor(definition("test.read", ToolMode.READ_ONLY, Set.of("ADMIN")));
        AgentToolExecutionContext context = context();

        AgentToolResult<Long> result =
                executor.execute(
                        "test.read",
                        context,
                        "读取任务 12",
                        () -> SecurityUtils.currentUser().getUserId());

        assertThat(result.success()).isTrue();
        assertThat(result.data()).isEqualTo(7L);
        verify(auditService)
                .record(eq(context), eq("test.read"), eq("读取任务 12"), any(), eq(null), eq(null));
    }

    @Test
    void executionFailureReturnsFailureAndWritesFailureLog() {
        AgentToolExecutor executor =
                executor(definition("test.read", ToolMode.READ_ONLY, Set.of("ADMIN")));
        AgentToolExecutionContext context = context();

        AgentToolResult<Object> result =
                executor.execute(
                        "test.read",
                        context,
                        "失败请求",
                        () -> {
                            throw new BusinessException("无权访问采集任务");
                        });

        assertThat(result.success()).isFalse();
        assertThat(result.summary()).isEqualTo("无权访问采集任务");
        verify(auditService)
                .record(
                        eq(context),
                        eq("test.read"),
                        eq("失败请求"),
                        any(),
                        eq("BUSINESS_ERROR"),
                        eq("无权访问采集任务"));
    }

    @Test
    void rejectsUnregisteredToolAndWritesFailureLog() {
        AgentToolExecutor executor = executor();
        AgentToolExecutionContext context = context();

        AgentToolResult<Object> result =
                executor.execute("missing.tool", context, "未知工具", () -> new Object());

        assertThat(result.success()).isFalse();
        assertThat(result.summary()).contains("未注册");
        verify(auditService)
                .record(
                        eq(context),
                        eq("missing.tool"),
                        eq("未知工具"),
                        any(),
                        eq("BUSINESS_ERROR"),
                        any());
    }

    @Test
    void rejectsWriteToolWithRequiredMessage() {
        AgentToolExecutor executor =
                executor(definition("test.write", ToolMode.WRITE, Set.of("ADMIN")));

        AgentToolResult<Object> result =
                executor.execute("test.write", context(), "写操作", Object::new);

        assertThat(result.success()).isFalse();
        assertThat(result.summary()).isEqualTo("当前阶段不允许 Agent 执行业务写操作。");
    }

    @Test
    void rejectsRoleOutsideDefinition() {
        authenticate(Set.of("COLLECTOR"));
        AgentToolExecutor executor =
                executor(definition("admin.read", ToolMode.READ_ONLY, Set.of("ADMIN")));

        AgentToolResult<Object> result =
                executor.execute("admin.read", context(), "管理员工具", Object::new);

        assertThat(result.success()).isFalse();
        assertThat(result.summary()).contains("无权调用");
    }

    @Test
    void sanitizerRemovesLocalPathsAndSensitiveFields() throws Exception {
        AgentToolLogSanitizer sanitizer = new AgentToolLogSanitizer(new ObjectMapper());
        String json =
                sanitizer.outputJson(
                        new SensitiveOutput(
                                "C:\\bdis\\storage\\secret.jpg",
                                "/tmp/vector.bin",
                                "should-not-log"));

        assertThat(json).doesNotContain("C:\\bdis", "/tmp/vector.bin", "should-not-log");
        assertThat(json).contains("已脱敏路径");
        assertThat(new ObjectMapper().readTree(json).has("rawResult")).isFalse();
    }

    @Test
    void sanitizerRemovesJwtAndApiKeysFromFreeText() {
        AgentToolLogSanitizer sanitizer = new AgentToolLogSanitizer(new ObjectMapper());
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";
        String apiKey = "sk-1234567890abcdefghijklmnop";

        String sanitized =
                sanitizer.text(
                        "Authorization: Bearer " + jwt + ", apiKey=" + apiKey + ", password=secret",
                        1000);

        assertThat(sanitized).doesNotContain(jwt, apiKey, "secret");
        assertThat(sanitized).contains("已脱敏");
    }

    private AgentToolExecutor executor(AgentToolDefinition... definitions) {
        AgentBusinessTool tool = () -> List.of(definitions);
        AgentToolRegistry registry = new AgentToolRegistry(List.of(tool));
        AsyncTaskExecutor secured = new DelegatingSecurityContextAsyncTaskExecutor(worker);
        return new AgentToolExecutor(registry, auditService, secured);
    }

    private AgentToolDefinition definition(String name, ToolMode mode, Set<String> requiredRoles) {
        return new AgentToolDefinition(name, "测试工具", "测试", mode, requiredRoles, RiskLevel.LOW, 5);
    }

    private AgentToolExecutionContext context() {
        return AgentToolExecutionContext.fromCurrentUser(
                new AgentToolExecutionContext.Scope(
                        "session-1", 1L, 2L, "digital-life", 12L, null, null, null, "req-1"));
    }

    private void authenticate(Set<String> roles) {
        CurrentUser user = new CurrentUser(7L, "tester", "测试用户", 1L, 1L, roles, Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private record SensitiveOutput(String imageUrl, String localPath, String rawResult) {}
}
