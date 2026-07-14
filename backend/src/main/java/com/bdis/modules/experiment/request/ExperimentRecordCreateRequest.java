package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ExperimentRecordCreateRequest {

    @NotBlank
    @Size(max = 64)
    private String recordNo;

    @Positive private Long courseId;

    @Positive private Long projectId;

    @NotBlank
    @Size(max = 200)
    private String experimentTitle;

    private String experimentProcess;

    private String experimentResult;

    private LocalDateTime recordedAt;

    @Size(max = 500)
    private String remark;

    @AssertTrue(message = "Exactly one of courseId and projectId is required")
    public boolean isSourceSelectionValid() {
        return (courseId == null) != (projectId == null);
    }
}
