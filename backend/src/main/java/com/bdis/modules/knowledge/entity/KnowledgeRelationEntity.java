package com.bdis.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_knowledge_relation")
public class KnowledgeRelationEntity extends BaseEntity {

    private String relationCode;

    private String relationName;

    private String description;
}
