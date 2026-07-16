package com.bdis.modules.assistant.vo;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HerbAssistantRagChatResponse {

    private String sessionId;

    private String answer;

    private List<HerbAssistantRagReferenceVO> references;
}
