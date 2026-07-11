package com.bdis.audit.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class LoginLogVO {

    private Long id;
    private Long userId;
    private String username;
    private String loginResult;
    private String failureReason;
    private String ipAddress;
    private LocalDateTime loggedInAt;
}
