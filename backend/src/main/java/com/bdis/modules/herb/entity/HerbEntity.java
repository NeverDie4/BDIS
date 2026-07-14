package com.bdis.modules.herb.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_species")
public class HerbEntity extends BaseEntity {

    private String herbNo;

    private String herbName;

    private String aliasName;

    private String latinName;

    private Long categoryId;

    private String categoryCode;

    private String medicinalPart;

    private String efficacy;

    private String growthEnvironment;

    private String originArea;

    private String growthCycle;

    private String description;

    private Long knowledgeEntityId;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private String distributionRegionText;
}
