package com.bdis.modules.assistant.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import lombok.Data;

@Data
public class AgentTaskQueryRequest {

    private String status;
    private String goalType;

    @Positive(message = "采集任务 ID 必须大于0")
    private Long collectionTaskId;

    @Min(value = 1, message = "页码不能小于1")
    private Integer page = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 200, message = "每页数量不能超过200")
    private Integer size = 10;
}
