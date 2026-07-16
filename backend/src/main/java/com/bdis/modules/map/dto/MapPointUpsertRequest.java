package com.bdis.modules.map.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MapPointUpsertRequest {

    private Long speciesId;

    @NotBlank(message = "药材名称不能为空")
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

    private Long regionId;

    private String locationName;

    @NotNull(message = "经度不能为空")
    @DecimalMin(value = "-180.0", message = "经度不能小于 -180")
    @DecimalMax(value = "180.0", message = "经度不能大于 180")
    private BigDecimal longitude;

    @NotNull(message = "纬度不能为空")
    @DecimalMin(value = "-90.0", message = "纬度不能小于 -90")
    @DecimalMax(value = "90.0", message = "纬度不能大于 90")
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

    private String remark;
}
