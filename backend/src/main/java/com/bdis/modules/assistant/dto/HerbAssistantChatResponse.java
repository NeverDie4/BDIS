package com.bdis.modules.assistant.dto;

import com.bdis.modules.assistant.vo.HerbAssistantRagReferenceVO;
import java.util.List;
import lombok.Data;

@Data
public class HerbAssistantChatResponse {

    private String sessionId;

    private String answer;

    private Boolean ragUsed;

    private List<HerbAssistantRagReferenceVO> references;

    public HerbAssistantChatResponse(String sessionId, String answer) {
        this(sessionId, answer, false, List.of());
    }

    public HerbAssistantChatResponse(
            String sessionId,
            String answer,
            Boolean ragUsed,
            List<HerbAssistantRagReferenceVO> references) {
        this.sessionId = sessionId;
        this.answer = answer;
        this.ragUsed = ragUsed;
        this.references = references == null ? List.of() : references;
    }
}
