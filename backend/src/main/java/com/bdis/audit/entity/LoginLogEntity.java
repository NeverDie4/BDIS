package com.bdis.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("log_login")
public class LoginLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String username;
    private String loginResult;
    private String failureReason;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime loggedInAt;
    private LocalDateTime createdAt;
}
