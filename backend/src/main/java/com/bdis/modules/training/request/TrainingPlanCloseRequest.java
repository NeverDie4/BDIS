package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrainingPlanCloseRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;

    @NotNull private Integer version;
}
