package com.bdis.modules.assistant.knowledge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class HerbAiKnowledgeDocQueryRequest {

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 200, message = "每页数量不能超过200")
    private Integer pageSize = 10;

    private String keyword;
    private String docType;
    private String status;
    private String embeddingStatus;
}
