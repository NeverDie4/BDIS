package com.bdis.modules.collection.dto;

import lombok.Data;

@Data
public class HerbBatchStatusChangeRequest {

    private Boolean force;

    private String reason;

    private String remark;

    private Long operatorId;

    private String operatorName;
}
