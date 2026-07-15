package com.bdis.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("sys_notification")
public class NotificationEntity {
    private Long id;
    private Long recipientId;
    private String notificationType;
    private String bizType;
    private Long bizId;
    private String title;
    private String content;
    private Integer readStatus;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
