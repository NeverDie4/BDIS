package com.bdis.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class MobileAsyncConfigTest {

    private ThreadPoolTaskExecutor delegate;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (delegate != null) {
            delegate.shutdown();
        }
    }

    @Test
    void mobileIdentificationExecutorPropagatesCurrentUserToWorkerThread() throws Exception {
        MobileAsyncConfig config = new MobileAsyncConfig();
        delegate = config.mobileAutoIdentificationDelegate();
        AsyncTaskExecutor executor = config.mobileAutoIdentificationExecutor(delegate);
        CurrentUser currentUser =
                new CurrentUser(
                        7L,
                        "collector",
                        "采集员",
                        1L,
                        1L,
                        Set.of("COLLECTOR"),
                        Set.of(1L),
                        Set.of("herb:identification:execute"));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));

        Long asyncUserId =
                executor.submitCompletable(() -> SecurityUtils.currentUser().getUserId())
                        .get(5, TimeUnit.SECONDS);

        assertThat(asyncUserId).isEqualTo(7L);
    }
}
