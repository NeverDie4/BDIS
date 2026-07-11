package com.bdis.modules.assistant.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HerbAssistantBatchExplainResponse {

    private Long batchId;

    private String sessionId;

    private String answer;
}
