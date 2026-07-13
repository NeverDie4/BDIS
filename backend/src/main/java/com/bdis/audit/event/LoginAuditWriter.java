package com.bdis.audit.event;

import com.bdis.modules.audit.entity.LoginLogEntity;
import com.bdis.modules.audit.mapper.LoginLogMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LoginAuditWriter {

    private final LoginLogMapper loginLogMapper;

    public LoginAuditWriter(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(LoginAuditEvent event) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUserId(event.userId());
        entity.setUsername(event.username());
        entity.setLoginResult(event.loginResult());
        entity.setFailReason(event.failureReason());
        entity.setIpAddress(event.ipAddress());
        entity.setUserAgent(event.userAgent());
        entity.setLoggedInAt(event.loggedInAt());
        loginLogMapper.insert(entity);
    }
}
