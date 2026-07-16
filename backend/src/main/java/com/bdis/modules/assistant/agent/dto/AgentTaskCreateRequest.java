package com.bdis.modules.assistant.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AgentTaskCreateRequest {

    @NotBlank(message = "目标类型不能为空")
    @Size(max = 64, message = "目标类型长度不能超过64个字符")
    private String goalType;

    @NotBlank(message = "目标描述不能为空")
    @Size(max = 1000, message = "目标描述长度不能超过1000个字符")
    private String goalText;

    @NotBlank(message = "业务目标类型不能为空")
    @Size(max = 64, message = "业务目标类型长度不能超过64个字符")
    private String targetType;

    @Positive(message = "业务目标 ID 必须大于0")
    private Long targetId;

    @NotNull(message = "采集任务 ID 不能为空")
    @Positive(message = "采集任务 ID 必须大于0")
    private Long collectionTaskId;

    @Size(max = 64, message = "会话 ID 长度不能超过64个字符")
    private String sessionId;

    @Size(max = 64, message = "页面上下文长度不能超过64个字符")
    private String pageContext;
}
