package com.bdis.modules.training.query;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingFeedbackQuery {
    @Positive private Long planId;
    @Positive private Long trainingRecordId;
    @Positive private Long userId;
    @DecimalMin("1.00") @DecimalMax("5.00") private BigDecimal rating;
    private LocalDateTime submittedFrom;
    private LocalDateTime submittedTo;
    @Min(1) private Integer pageNo = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
    private String sortField = "submittedAt";
    private String sortOrder = "desc";
}
