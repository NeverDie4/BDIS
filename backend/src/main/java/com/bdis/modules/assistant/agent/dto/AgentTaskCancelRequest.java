package com.bdis.modules.assistant.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

@Data
public class AgentTaskCancelRequest {

    @NotBlank(message = "取消原因不能为空")
    @Size(max = 500, message = "取消原因长度不能超过500个字符")
    private String reason;
}
