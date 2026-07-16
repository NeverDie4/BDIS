package com.bdis.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_knowledge_triple")
public class KnowledgeTripleEntity extends BaseEntity {

    private Long subjectEntityId;

    private Long relationId;

    private Long objectEntityId;

    private BigDecimal confidence;

    private String sourceType;

    private String sourceDesc;
}
