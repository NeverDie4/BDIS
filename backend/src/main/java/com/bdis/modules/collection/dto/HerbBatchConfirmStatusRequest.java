package com.bdis.modules.collection.dto;

import lombok.Data;

@Data
public class HerbBatchConfirmStatusRequest {

    private Boolean force;

    private String remark;

    private Long operatorId;

    private String operatorName;
}
