package com.bdis.modules.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_knowledge_triple")
public class KnowledgeTripleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long subjectEntityId;

    private Long relationId;

    private Long objectEntityId;

    private BigDecimal confidence;

    private String source;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
