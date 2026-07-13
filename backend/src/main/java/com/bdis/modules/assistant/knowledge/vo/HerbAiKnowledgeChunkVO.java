package com.bdis.modules.assistant.knowledge.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAiKnowledgeChunkVO {

    private Long id;
    private Long docId;
    private Integer chunkIndex;
    private String chunkTitle;
    private String chunkContent;
    private String contentHash;
    private Integer tokenCount;
    private String embeddingStatus;
    private String vectorId;
    private String metadataJson;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
