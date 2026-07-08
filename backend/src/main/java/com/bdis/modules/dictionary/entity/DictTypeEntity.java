package com.bdis.modules.dictionary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dict_type")
public class DictTypeEntity extends BaseEntity {

    private String typeCode;

    private String typeName;

    private Integer sortOrder;
}
