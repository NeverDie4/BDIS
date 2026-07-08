package com.bdis.modules.dictionary.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("dict_region")
public class RegionEntity extends BaseEntity {

    private String regionCode;

    private String regionName;

    private Long parentId;

    private String regionLevel;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer sortOrder;
}
