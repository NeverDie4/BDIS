package com.bdis.modules.map.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_distribution")
public class MapPointEntity extends BaseEntity {

    private Long speciesId;

    private Long baseId;

    private Long regionId;

    private String locationName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String province;

    private String city;

    private String district;

    private String address;

    private BigDecimal altitude;

    private String distributionType;

    private String distributionLevel;

    private String distributionDesc;

    private String coverImageUrl;

    private LocalDateTime lastCollectedAt;

    private String sourceType;

    private String dataSource;
}
