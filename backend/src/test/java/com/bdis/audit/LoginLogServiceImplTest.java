package com.bdis.audit;

import static org.mockito.Mockito.verify;

import com.bdis.audit.event.LoginAuditPublisher;
import com.bdis.audit.service.impl.LoginLogServiceImpl;
import com.bdis.modules.audit.mapper.LoginLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTest {

    @Mock private LoginLogMapper loginLogMapper;

    @Mock private LoginAuditPublisher loginAuditPublisher;

    @InjectMocks private LoginLogServiceImpl service;

    @Test
    void recordDelegatesToTransactionAwarePublisher() {
        service.record(7L, "teacher01", "success", null);

        verify(loginAuditPublisher).publish(7L, "teacher01", "success", null);
    }
}
