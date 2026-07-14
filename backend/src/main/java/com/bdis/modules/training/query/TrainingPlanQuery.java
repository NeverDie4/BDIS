package com.bdis.modules.training.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanQuery {
    private String keyword;
    private String planType;
    private String publishStatus;
    @Positive private Long ownerId;
    @Positive private Long trainerId;
    private LocalDateTime startedFrom;
    private LocalDateTime startedTo;
    private LocalDateTime endedFrom;
    private LocalDateTime endedTo;

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;
}
