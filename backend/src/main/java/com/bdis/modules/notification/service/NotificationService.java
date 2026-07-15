package com.bdis.modules.notification.service;

import com.bdis.modules.notification.entity.NotificationEntity;
import java.util.List;

public interface NotificationService {
    void create(
            Long recipientId,
            String type,
            String bizType,
            Long bizId,
            String title,
            String content);

    List<NotificationEntity> listMine(Integer limit);

    void read(Long id);

    void readAll();
}
