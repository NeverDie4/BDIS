package com.bdis.modules.training.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingRecordQuery {
    private String keyword;
    @Positive private Long planId;
    @Positive private Long userId;
    private String trainingStatus;
    private String attendanceStatus;
    private LocalDateTime recordedFrom;
    private LocalDateTime recordedTo;
    @Min(1) private Integer pageNo = 1;
    @Min(1) @Max(100) private Integer pageSize = 10;
}
