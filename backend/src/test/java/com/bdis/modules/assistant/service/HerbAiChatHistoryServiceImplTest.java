package com.bdis.modules.assistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.entity.HerbAiChatMessage;
import com.bdis.modules.assistant.entity.HerbAiChatSession;
import com.bdis.modules.assistant.mapper.HerbAiChatMessageMapper;
import com.bdis.modules.assistant.mapper.HerbAiChatSessionMapper;
import com.bdis.modules.assistant.service.impl.HerbAiChatHistoryServiceImpl;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class HerbAiChatHistoryServiceImplTest {

    @Mock private HerbAiChatSessionMapper sessionMapper;
    @Mock private HerbAiChatMessageMapper messageMapper;

    private HerbAiChatHistoryService service;

    @BeforeEach
    void setUp() {
        service = new HerbAiChatHistoryServiceImpl(sessionMapper, messageMapper);
        CurrentUser currentUser =
                new CurrentUser(1L, "user-1", "User One", null, null, Set.of(), Set.of(), Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordUserMessageCreatesSessionAndStoresUserMessage() {
        HerbAssistantChatRequest request = new HerbAssistantChatRequest();
        request.setMessage("这是一个超过三十个字的会话标题问题用于验证标题会被自动截断到规定长度");
        request.setSource("mobile");
        when(sessionMapper.selectBySessionId(any(), eq(1L))).thenReturn(null);

        String sessionId = service.recordUserMessage(request);

        assertThat(sessionId).isNotBlank();
        verify(sessionMapper).insert(any());
        ArgumentCaptor<HerbAiChatMessage> messageCaptor =
                ArgumentCaptor.forClass(HerbAiChatMessage.class);
        verify(messageMapper).insert(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getRole()).isEqualTo("user");
        assertThat(messageCaptor.getValue().getSource()).isEqualTo("mobile");
        verify(sessionMapper).updateLastMessage(any(), eq(1L), any(), any());
    }

    @Test
    void deleteSessionLogicallyDeletesMessagesAndSession() {
        when(sessionMapper.selectBySessionId("session-1", 1L))
                .thenReturn(new com.bdis.modules.assistant.entity.HerbAiChatSession());

        service.deleteSession("session-1");

        verify(messageMapper).logicDeleteBySessionId("session-1", 1L);
        verify(sessionMapper).logicDeleteBySessionId("session-1", 1L);
    }

    @Test
    void recordUserMessageIgnoresForgedUserIdAndUsesAuthenticatedUser() {
        HerbAssistantChatRequest request = new HerbAssistantChatRequest();
        request.setMessage("How do I upload an image?");
        request.setUserId(999L);
        when(sessionMapper.selectBySessionId(any(), eq(1L))).thenReturn(null);

        service.recordUserMessage(request);

        ArgumentCaptor<HerbAiChatSession> sessionCaptor =
                ArgumentCaptor.forClass(HerbAiChatSession.class);
        verify(sessionMapper).insert(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getUserId()).isEqualTo(1L);
    }

    @Test
    void listMessagesRejectsSessionOwnedByAnotherUser() {
        when(sessionMapper.selectBySessionId("other-session", 1L)).thenReturn(null);

        assertThatThrownBy(() -> service.listMessages("other-session"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("AI 会话不存在");
    }
}
