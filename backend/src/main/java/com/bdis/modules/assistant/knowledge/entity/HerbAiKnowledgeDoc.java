package com.bdis.modules.assistant.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_ai_knowledge_doc")
public class HerbAiKnowledgeDoc {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableField("is_deleted")
    private Integer deleted;
}
