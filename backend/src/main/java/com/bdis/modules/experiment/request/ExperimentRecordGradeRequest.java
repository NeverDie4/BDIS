package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExperimentRecordGradeRequest {

    @NotNull @PositiveOrZero private Integer version;

    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal score;

    @Size(max = 1000)
    private String gradeComment;
}
