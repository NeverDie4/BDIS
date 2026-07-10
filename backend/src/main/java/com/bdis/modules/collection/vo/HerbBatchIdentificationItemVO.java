package com.bdis.modules.collection.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbBatchIdentificationItemVO {

    private Long batchImageId;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private String imageRole;

    private Integer isPrimary;

    private Long identificationResultId;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Boolean needReview;

    private String reviewStatus;

    private String suggestion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime identifyTime;
}
