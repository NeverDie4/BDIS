package com.bdis.modules.training.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Null;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingRecordUpdateRequest {
    @Null(message = "planId cannot be changed") private Long planId;
    @Null(message = "userId cannot be changed") private Long userId;
    @Null(message = "courseId cannot be changed") private Long courseId;
    @Size(max = 50) private String attendanceStatus;
    @Size(max = 50) private String trainingStatus;
    @DecimalMin("0.00") @DecimalMax("100.00") private BigDecimal progress;
    @DecimalMin("0.00") @DecimalMax("100.00") private BigDecimal score;
    private LocalDateTime startedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime completedAt;
    @Size(max = 500) private String resultComment;
    @Size(max = 500) private String remark;
}
