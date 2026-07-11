package com.bdis.modules.assistant.service;

import com.bdis.modules.assistant.dto.HerbAssistantChatRequest;
import com.bdis.modules.assistant.dto.HerbAssistantChatResponse;
import com.bdis.modules.assistant.dto.HerbAssistantBatchExplainRequest;
import com.bdis.modules.assistant.dto.HerbAssistantImageExplainRequest;
import com.bdis.modules.assistant.vo.HerbAssistantBatchExplainResponse;
import com.bdis.modules.assistant.vo.HerbAssistantImageExplainResponse;

public interface HerbAssistantService {

    HerbAssistantChatResponse chat(HerbAssistantChatRequest request);

    HerbAssistantBatchExplainResponse explainBatch(
            Long batchId, HerbAssistantBatchExplainRequest request);

    HerbAssistantImageExplainResponse explainImage(
            Long imageId, HerbAssistantImageExplainRequest request);
}
