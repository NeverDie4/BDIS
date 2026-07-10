package com.bdis.modules.map.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_base")
public class HerbBaseEntity extends BaseEntity {

    private String baseNo;

    private String baseName;

    private String baseType;

    private Long regionId;

    private String address;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String contactName;

    private String contactPhone;

    private String description;
}
