package com.bdis.modules.training.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TrainingMaterialQuery {
    private String keyword;
    private String materialType;
    private String sourceType;
    @Positive private Long uploaderId;
    @Min(0) @Max(1) private Integer status;
    @Min(1) private Integer pageNo = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private String sortField;
    private String sortOrder;
}
