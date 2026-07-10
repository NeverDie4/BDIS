package com.bdis.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_knowledge_entity")
public class KnowledgeNodeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String entityCode;

    private String entityName;

    private String entityType;

    private Long relatedId;

    private String description;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
