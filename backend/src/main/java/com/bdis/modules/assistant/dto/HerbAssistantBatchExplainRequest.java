package com.bdis.modules.assistant.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HerbAssistantBatchExplainRequest {

    @Size(max = 1000, message = "问题长度不能超过1000字")
    private String question;

    private String sessionId;

    private Long userId;

    private String source;
}
