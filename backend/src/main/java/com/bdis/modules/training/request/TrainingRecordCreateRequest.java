package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrainingRecordCreateRequest {
    @NotNull @Positive private Long planId;
    @NotNull @Positive private Long userId;

    @Size(max = 500)
    private String remark;
}
