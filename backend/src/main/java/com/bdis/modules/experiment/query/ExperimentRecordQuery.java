package com.bdis.modules.experiment.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentRecordQuery {

    private String keyword;

    @Positive private Long courseId;

    @Positive private Long projectId;

    @Positive private Long recorderId;

    private String archiveStatus;

    private LocalDateTime recordedFrom;

    private LocalDateTime recordedTo;

    @Min(1)
    private Integer pageNo = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 10;
}
