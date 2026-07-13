package com.bdis.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bdis.audit.service.impl.LoginLogServiceImpl;
import com.bdis.modules.audit.entity.LoginLogEntity;
import com.bdis.modules.audit.mapper.LoginLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginLogServiceImplTest {

    @Mock private LoginLogMapper loginLogMapper;

    @InjectMocks private LoginLogServiceImpl service;

    @Test
    void recordNormalizesResultToUppercase() {
        service.record(7L, "teacher01", "success", null);

        ArgumentCaptor<LoginLogEntity> log = ArgumentCaptor.forClass(LoginLogEntity.class);
        verify(loginLogMapper).insert(log.capture());
        assertThat(log.getValue().getLoginResult()).isEqualTo("SUCCESS");
    }
}
