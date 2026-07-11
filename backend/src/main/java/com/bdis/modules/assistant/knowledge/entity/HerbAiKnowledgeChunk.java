package com.bdis.modules.assistant.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_ai_knowledge_chunk")
public class HerbAiKnowledgeChunk {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
