package com.bdis.modules.mobile.vo;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class MobileBatchImageUploadResultVO {

    private Long batchId;

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

    private Boolean autoIdentifySuccess;

    private String message;
}
