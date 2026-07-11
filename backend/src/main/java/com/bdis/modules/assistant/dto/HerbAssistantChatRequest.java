package com.bdis.modules.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HerbAssistantChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 1000, message = "消息长度不能超过1000字")
    private String message;

    private String sessionId;

    private Long userId;

    private String source;

    private Boolean useRag;

    private String docType;

    private Integer topK;
}
