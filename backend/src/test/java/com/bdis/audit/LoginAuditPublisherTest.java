package com.bdis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.bdis.audit.event.LoginAuditEvent;
import com.bdis.audit.event.LoginAuditPublisher;
import com.bdis.audit.event.LoginAuditWriter;
import com.bdis.audit.support.AuditPersistenceScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAuditPublisherTest {

    @Mock private AuditPersistenceScheduler scheduler;

    @Mock private LoginAuditWriter writer;

    @Test
    void failedLoginIsScheduledAfterTransactionCompletion() {
        doAnswer(
                        invocation -> {
                            invocation.<Runnable>getArgument(1).run();
                            return null;
                        })
                .when(scheduler)
                .afterCompletion(eq("login"), org.mockito.ArgumentMatchers.any(Runnable.class));
        LoginAuditPublisher publisher = new LoginAuditPublisher(scheduler, writer);

        publisher.publish(null, "missing", "failed", "账号或密码错误");

        ArgumentCaptor<LoginAuditEvent> event = ArgumentCaptor.forClass(LoginAuditEvent.class);
        verify(writer).write(event.capture());
        assertThat(event.getValue().loginResult()).isEqualTo("FAILED");
        assertThat(event.getValue().failureReason()).isEqualTo("账号或密码错误");
    }
}
