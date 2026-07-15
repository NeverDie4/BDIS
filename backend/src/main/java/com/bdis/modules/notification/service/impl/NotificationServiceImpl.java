package com.bdis.modules.notification.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.utils.CurrentUserUtils;
import com.bdis.modules.notification.entity.NotificationEntity;
import com.bdis.modules.notification.mapper.NotificationMapper;
import com.bdis.modules.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper mapper;

    public NotificationServiceImpl(NotificationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public void create(
            Long recipientId,
            String type,
            String bizType,
            Long bizId,
            String title,
            String content) {
        if (recipientId == null || title == null || title.isBlank()) {
            return;
        }
        NotificationEntity e = new NotificationEntity();
        e.setRecipientId(recipientId);
        e.setNotificationType(type);
        e.setBizType(bizType);
        e.setBizId(bizId);
        e.setTitle(title);
        e.setContent(content);
        e.setReadStatus(0);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e.setVersion(0);
        mapper.insert(e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationEntity> listMine(Integer limit) {
        Long id = requireUser();
        int safe = limit == null ? 50 : Math.min(Math.max(limit, 1), 100);
        return mapper.selectMine(id, safe);
    }

    @Override
    @Transactional
    public void read(Long id) {
        if (mapper.markRead(id, requireUser()) == 0) {
            throw new BusinessException("Notification not found or already read");
        }
    }

    @Override
    @Transactional
    public void readAll() {
        mapper.markAllRead(requireUser());
    }

    private Long requireUser() {
        Long id = CurrentUserUtils.currentUserId();
        if (id == null) {
            throw new ForbiddenException("Authentication is required");
        }
        return id;
    }
}
