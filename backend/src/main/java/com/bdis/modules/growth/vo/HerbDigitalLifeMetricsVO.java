package com.bdis.modules.growth.vo;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HerbDigitalLifeMetricsVO {

    private BigDecimal plantHeight;

    private BigDecimal stemDiameter;

    private String leafColor;

    private String floweringStatus;

    private BigDecimal temperature;

    private BigDecimal humidity;

    private BigDecimal soilMoisture;

    private BigDecimal soilPh;

    private BigDecimal light;

    private String growthEvaluation;

    private String remark;
}
