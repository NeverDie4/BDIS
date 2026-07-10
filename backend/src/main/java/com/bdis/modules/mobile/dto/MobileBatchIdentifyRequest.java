package com.bdis.modules.mobile.dto;

import lombok.Data;

@Data
public class MobileBatchIdentifyRequest {

    private Boolean forceRefresh = false;

    private Integer topK;
}
