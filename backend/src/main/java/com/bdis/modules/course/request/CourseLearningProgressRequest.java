package com.bdis.modules.course.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CourseLearningProgressRequest {
    @NotBlank private String itemType;
    @NotNull @Positive private Long itemId;
    @DecimalMin("0") @DecimalMax("100") private java.math.BigDecimal progressValue;
    private Integer progressSeconds;
    private Integer totalSeconds;
    @NotNull private Boolean completed;
}
