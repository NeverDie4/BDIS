package com.bdis.modules.experiment.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExperimentRecordAttachmentBindRequest {

    @NotNull(message = "fileId is required")
    @Positive(message = "fileId must be positive")
    private Long fileId;

    @NotBlank(message = "fileUsage is required")
    @Pattern(
            regexp = "attachment|image|report",
            message = "fileUsage must be attachment, image or report")
    private String fileUsage;

    @Min(value = 0, message = "sortOrder must not be negative")
    private Integer sortOrder;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
