package com.bdis.modules.collection.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HerbBatchImageBindRequest {

    @NotNull private Long imageId;

    private Long identificationResultId;

    private String imageRole;

    private Integer isPrimary;

    private Integer sortOrder;

    private String remark;
}
