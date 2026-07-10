package com.bdis.modules.dictionary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dict_item")
public class DictItemEntity extends BaseEntity {

    private Long typeId;

    private String itemCode;

    private String itemName;

    private String itemValue;

    private Long parentId;

    private Integer sortOrder;
}
