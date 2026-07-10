package com.bdis.modules.collection.dto;

import lombok.Data;

@Data
public class HerbBatchImageUpdateRequest {

    private String imageRole;

    private Integer sortOrder;

    private String remark;
}
