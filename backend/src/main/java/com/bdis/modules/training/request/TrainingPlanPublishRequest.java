package com.bdis.modules.training.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TrainingPlanPublishRequest {
    @NotNull
    @Min(0)
    private Integer version;
}
