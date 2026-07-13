package com.bdis.modules.settings.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSessionVO {

    private String sessionId;

    private boolean current;

    private String clientType;

    private String deviceName;

    private String ipAddress;

    private LocalDateTime issuedAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime expiresAt;

    private String status;
}
