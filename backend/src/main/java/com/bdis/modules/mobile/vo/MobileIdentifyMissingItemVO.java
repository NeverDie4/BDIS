package com.bdis.modules.mobile.vo;

import lombok.Data;

@Data
public class MobileIdentifyMissingItemVO {

    private Long imageId;

    private Long batchImageId;

    private Boolean success;

    private Long identificationResultId;

    private String finalSpeciesName;

    private String message;
}
