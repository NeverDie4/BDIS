package com.bdis.modules.map.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapPointVO {

    private Long id;

    private Long speciesId;

    private String herbName;

    private String aliasName;

    private String latinName;

    private String medicinalPart;

    private String efficacy;

    private String growthEnvironment;

    private String originArea;

    private String growthCycle;

    private String herbDescription;

    private Long baseId;

    private String baseName;

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

    private Integer status;

    private String remark;
}
