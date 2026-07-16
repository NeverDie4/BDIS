package com.bdis.modules.growth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrowthRecordCreateRequest {

    @NotBlank(message = "采集者不能为空")
    private String collectorName;

    private LocalDateTime collectedAt;

    private String growthStage;

    private String weather;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private String soilType;

    private BigDecimal soilPh;

    @DecimalMin(value = "0.0", message = "采集重量不能小于 0")
    private BigDecimal sampleWeight;

    private String dataSource;

    private String remark;
}
