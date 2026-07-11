package com.bdis.modules.assistant.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.dto.HerbAiChatHistoryQueryRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.entity.HerbAiChatMessage;
import com.bdis.modules.assistant.entity.HerbAiChatSession;
import com.bdis.modules.assistant.mapper.HerbAiChatMessageMapper;
import com.bdis.modules.assistant.mapper.HerbAiChatSessionMapper;
import com.bdis.modules.assistant.service.HerbAiChatHistoryService;
import com.bdis.modules.assistant.vo.HerbAiChatMessageVO;
import com.bdis.modules.assistant.vo.HerbAiChatSessionVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbAiChatHistoryServiceImpl implements HerbAiChatHistoryService {

    private final HerbAiChatSessionMapper sessionMapper;
    private final HerbAiChatMessageMapper messageMapper;

    public HerbAiChatHistoryServiceImpl(
            HerbAiChatSessionMapper sessionMapper, HerbAiChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional
    public String recordUserMessage(HerbAssistantChatRequest request) {
        String sessionId =
                StringUtils.hasText(request.getSessionId())
                        ? request.getSessionId().trim()
                        : UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        if (sessionMapper.selectBySessionId(sessionId, userId) == null) {
            HerbAiChatSession session = new HerbAiChatSession();
            session.setSessionId(sessionId);
            session.setSessionTitle(abbreviate(request.getMessage().trim(), 30));
            session.setUserId(userId);
            session.setSource(normalize(request.getSource()));
            session.setStatus("normal");
            session.setCreateTime(now);
            session.setUpdateTime(now);
            session.setDeleted(0);
            sessionMapper.insert(session);
        }

        insertMessage(
                sessionId,
                "user",
                request.getMessage().trim(),
                null,
                normalize(request.getSource()),
                userId,
                now);
        sessionMapper.updateLastMessage(
                sessionId, userId, abbreviate(request.getMessage().trim(), 500), now);
        return sessionId;
    }

    @Override
    @Transactional
    public void recordAssistantMessage(
            String sessionId, String answer, String modelName, String source) {
        LocalDateTime now = LocalDateTime.now();
        Long userId = currentUserId();
        requireSession(sessionId, userId);
        insertMessage(sessionId, "assistant", answer, modelName, normalize(source), userId, now);
        sessionMapper.updateLastMessage(sessionId, userId, abbreviate(answer, 500), now);
    }

    @Override
    public PageResult<HerbAiChatSessionVO> listSessions(HerbAiChatHistoryQueryRequest request) {
        request.setUserId(currentUserId());
        int pageNum =
                request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize =
                request.getPageSize() == null || request.getPageSize() < 1
                        ? 10
                        : Math.min(request.getPageSize(), 200);
        long total = sessionMapper.countPage(request);
        long offset = (long) (pageNum - 1) * pageSize;
        List<HerbAiChatSessionVO> records = sessionMapper.selectPage(request, offset, pageSize);
        return new PageResult<>(records, pageNum, pageSize, total);
    }

    @Override
    public List<HerbAiChatMessageVO> listMessages(String sessionId) {
        requireSession(sessionId, currentUserId());
        return messageMapper.selectBySessionId(sessionId, currentUserId());
    }

    @Override
    @Transactional
    public void deleteSession(String sessionId) {
        Long userId = currentUserId();
        requireSession(sessionId, userId);
        messageMapper.logicDeleteBySessionId(sessionId, userId);
        sessionMapper.logicDeleteBySessionId(sessionId, userId);
    }

    private void insertMessage(
            String sessionId,
            String role,
            String content,
            String modelName,
            String source,
            Long userId,
            LocalDateTime now) {
        HerbAiChatMessage message = new HerbAiChatMessage();
        message.setSessionId(sessionId);
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(content);
        message.setModelName(modelName);
        message.setSource(source);
        message.setCreateTime(now);
        message.setUpdateTime(now);
        message.setDeleted(0);
        messageMapper.insert(message);
    }

    private HerbAiChatSession requireSession(String sessionId, Long userId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new BusinessException("会话 ID 不能为空");
        }
        HerbAiChatSession session = sessionMapper.selectBySessionId(sessionId.trim(), userId);
        if (session == null) {
            throw new BusinessException("AI 会话不存在");
        }
        return session;
    }

    private Long currentUserId() {
        return SecurityUtils.currentUser().getUserId();
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String abbreviate(String value, int maxCodePoints) {
        int count = value.codePointCount(0, value.length());
        if (count <= maxCodePoints) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxCodePoints));
    }
}
