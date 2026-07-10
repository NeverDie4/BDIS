package com.bdis.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_knowledge_entity")
public class KnowledgeNodeEntity extends BaseEntity {

    private String entityNo;

    private String entityName;

    private String entityType;

    private String refTable;

    private Long refId;

    private String description;
}
