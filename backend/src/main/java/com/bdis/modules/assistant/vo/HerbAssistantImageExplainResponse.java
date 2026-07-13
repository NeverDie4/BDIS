package com.bdis.modules.assistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HerbAssistantImageExplainResponse {

    private Long imageId;

    private String sessionId;

    private String answer;
}
