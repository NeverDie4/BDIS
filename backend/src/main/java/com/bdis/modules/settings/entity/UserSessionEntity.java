package com.bdis.modules.settings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_user_session")
public class UserSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private String deviceId;

    private String tokenJti;

    private String refreshTokenHash;

    private String clientType;

    private String deviceName;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime issuedAt;

    private LocalDateTime lastActiveAt;

    private LocalDateTime expiresAt;

    private LocalDateTime refreshExpiresAt;

    private LocalDateTime revokedAt;

    private String revokeReason;

    private String sessionStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
