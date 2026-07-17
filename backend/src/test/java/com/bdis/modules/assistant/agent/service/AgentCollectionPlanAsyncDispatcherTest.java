package com.bdis.modules.assistant.agent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

@ExtendWith(MockitoExtension.class)
class AgentCollectionPlanAsyncDispatcherTest {

    @Mock private AgentCollectionPlanService planService;

    private ThreadPoolTaskExecutor delegate;
    private AgentCollectionPlanAsyncDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(1);
        delegate.setMaxPoolSize(1);
        delegate.setQueueCapacity(2);
        delegate.setThreadNamePrefix("agent-plan-test-");
        delegate.initialize();
        dispatcher =
                new AgentCollectionPlanAsyncDispatcher(
                        planService, new DelegatingSecurityContextAsyncTaskExecutor(delegate));
        authenticate();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        delegate.shutdown();
    }

    @Test
    void triggerReturnsImmediatelyAndPropagatesSecurityContextToWorkerThread() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Authentication> workerAuthentication = new AtomicReference<>();
        org.mockito.Mockito.when(planService.generate(1L, false))
                .thenAnswer(
                        (Answer<Object>)
                                invocation -> {
                                    workerAuthentication.set(
                                            SecurityContextHolder.getContext()
                                                    .getAuthentication());
                                    entered.countDown();
                                    assertThat(release.await(3, TimeUnit.SECONDS)).isTrue();
                                    return null;
                                });

        dispatcher.triggerGenerate(1L, false);
        dispatcher.triggerGenerate(1L, false);

        assertThat(entered.await(3, TimeUnit.SECONDS)).isTrue();
        Authentication authentication = workerAuthentication.get();
        assertThat(authentication).isNotNull();
        assertThat(((CurrentUser) authentication.getPrincipal()).getUserId()).isEqualTo(7L);

        release.countDown();
        awaitExecutorIdle();
        verify(planService, times(1)).generate(1L, false);
    }

    @Test
    void invalidGenerationStateFailsBeforeAsyncDispatch() {
        doThrow(new BusinessException("当前任务正在等待现场数据，不能重新生成复测方案"))
                .when(planService)
                .validateGenerationRequest(6L, false);

        assertThatThrownBy(() -> dispatcher.triggerGenerate(6L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("等待现场数据");

        verify(planService, never()).generate(6L, false);
        assertThat(delegate.getThreadPoolExecutor().getActiveCount()).isZero();
    }

    private void awaitExecutorIdle() throws InterruptedException {
        for (int i = 0; i < 30; i++) {
            if (delegate.getThreadPoolExecutor().getActiveCount() == 0) {
                return;
            }
            Thread.sleep(100);
        }
    }

    private void authenticate() {
        CurrentUser user =
                new CurrentUser(7L, "teacher", "教师", 1L, 1L, Set.of("TEACHER"), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
}
