package com.bdis.modules.assistant.service;

import com.bdis.modules.assistant.dto.HerbAssistantRagChatRequest;
import com.bdis.modules.assistant.vo.HerbAssistantRagChatResponse;

public interface HerbAssistantRagService {

    HerbAssistantRagChatResponse chat(HerbAssistantRagChatRequest request);
}
