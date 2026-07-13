package com.bdis.modules.training.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrainingPlanMaterialBindRequest {
    @NotNull @Positive private Long materialId;
    @Min(0) @jakarta.validation.constraints.Max(1) private Integer isRequired;
    @Min(0) private Integer sortOrder;
    @Size(max = 500) private String remark;
}
