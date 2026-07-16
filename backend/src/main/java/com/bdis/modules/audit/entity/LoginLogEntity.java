package com.bdis.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("log_login")
public class LoginLogEntity extends LogEntity {

    private Long userId;

    private String username;

    private String loginResult;

    private String failReason;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime loggedInAt;
}
