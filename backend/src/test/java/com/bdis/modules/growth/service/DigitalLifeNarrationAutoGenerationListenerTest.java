package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.growth.service.impl.DigitalLifeNarrationAutoGenerationListener;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

class DigitalLifeNarrationAutoGenerationListenerTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void securityContextReachesTheRealNarrationWorkerThread() throws Exception {
        DigitalLifeNarrationService narrationService = mock(DigitalLifeNarrationService.class);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<CurrentUser> workerUser = new AtomicReference<>();
        doAnswer(
                        invocation -> {
                            workerUser.set(SecurityUtils.currentUser());
                            completed.countDown();
                            return null;
                        })
                .when(narrationService)
                .generateForTask(6L);

        CurrentUser reviewer =
                new CurrentUser(
                        2L,
                        "reviewer",
                        "审核员",
                        null,
                        null,
                        Set.of("REVIEWER"),
                        Set.of(5L),
                        Set.of("growth:record:view", "growth:record:audit"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(reviewer, null, Set.of()));

        ThreadPoolTaskExecutor delegate = new ThreadPoolTaskExecutor();
        delegate.setCorePoolSize(1);
        delegate.setMaxPoolSize(1);
        delegate.setThreadNamePrefix("narration-security-test-");
        delegate.initialize();
        try {
            AsyncTaskExecutor executor = new DelegatingSecurityContextAsyncTaskExecutor(delegate);
            DigitalLifeNarrationAutoGenerationListener listener =
                    new DigitalLifeNarrationAutoGenerationListener(narrationService);

            executor.submit(
                    () ->
                            listener.generate(
                                    new DigitalLifeNarrationAutoGenerationListener.Requested(6L)));

            assertThat(completed.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(workerUser.get()).isNotNull();
            assertThat(workerUser.get().getUserId()).isEqualTo(2L);
        } finally {
            delegate.shutdown();
        }

        Async async =
                DigitalLifeNarrationAutoGenerationListener.class
                        .getMethod(
                                "generate",
                                DigitalLifeNarrationAutoGenerationListener.Requested.class)
                        .getAnnotation(Async.class);
        assertThat(async.value()).isEqualTo("agentToolSecurityExecutor");
    }
}
