package com.bdis.modules.assistant.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HerbAiKnowledgeDocUpdateRequest {

    @Size(max = 200, message = "文档标题长度不能超过200字")
    private String docTitle;

    private String docType;
    private String contentText;

    @Size(max = 1000, message = "文档摘要长度不能超过1000字")
    private String summary;

    private String status;

    @Size(max = 500, message = "备注长度不能超过500字")
    private String remark;
}
