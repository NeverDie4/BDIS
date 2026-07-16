package com.bdis.modules.mobile.dto;

import lombok.Data;

@Data
public class MobileTaskQueryRequest {

    private Long collectorId;

    private String taskStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
