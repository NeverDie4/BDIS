package com.bdis.modules.course.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseResourceBindRequest {

    @NotNull(message = "fileId is required")
    private Long fileId;

    @NotBlank(message = "resourceName is required")
    @Size(max = 150, message = "resourceName must not exceed 150 characters")
    private String resourceName;

    @Size(max = 50, message = "resourceType must not exceed 50 characters")
    private String resourceType;

    @Min(value = 0, message = "sortOrder must not be negative")
    private Integer sortOrder;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
