package com.bdis.modules.mobile.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MobileBatchImageVO {

    private Long batchImageId;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private String imageRole;

    private Integer isPrimary;

    private Long identificationResultId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private Boolean needReview;

    private String reviewStatus;

    private String resultSource;

    private LocalDateTime identifyTime;
}
