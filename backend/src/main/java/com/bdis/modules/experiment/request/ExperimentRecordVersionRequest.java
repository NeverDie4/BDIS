package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExperimentRecordVersionRequest {
    private Long reportFileId;
    @NotBlank private String experimentTitle;
    @NotBlank private String experimentProcess;
    @NotBlank private String experimentResult;
}
