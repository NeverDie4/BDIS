package com.bdis.modules.assistant.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.assistant.dto.HerbAiChatHistoryQueryRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.vo.HerbAiChatMessageVO;
import com.bdis.modules.assistant.vo.HerbAiChatSessionVO;
import java.util.List;

public interface HerbAiChatHistoryService {

    String recordUserMessage(HerbAssistantChatRequest request);

    void recordAssistantMessage(String sessionId, String answer, String modelName, String source);

    PageResult<HerbAiChatSessionVO> listSessions(HerbAiChatHistoryQueryRequest request);

    List<HerbAiChatMessageVO> listMessages(String sessionId);

    void deleteSession(String sessionId);
}
