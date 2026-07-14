package com.bdis.modules.course.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExperimentStepCreateRequest {

    @NotBlank(message = "stepNo is required")
    @Size(max = 50, message = "stepNo must not exceed 50 characters")
    private String stepNo;

    @NotBlank(message = "stepTitle is required")
    @Size(max = 200, message = "stepTitle must not exceed 200 characters")
    private String stepTitle;

    private String stepContent;

    private String expectedResult;

    @Min(value = 0, message = "sortOrder must not be negative")
    private Integer sortOrder;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
