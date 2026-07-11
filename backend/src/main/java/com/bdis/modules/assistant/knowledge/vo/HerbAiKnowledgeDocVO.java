package com.bdis.modules.assistant.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAiKnowledgeDocVO {

    private Long id;
    private String docCode;
    private String docTitle;
    private String docType;
    private String sourceType;
    private String fileName;
    private String fileUrl;
    private String contentText;
    private String summary;
    private String status;
    private String embeddingStatus;
    private Integer chunkCount;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
