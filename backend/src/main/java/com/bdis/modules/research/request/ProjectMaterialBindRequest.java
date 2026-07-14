package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectMaterialBindRequest {
    @NotNull(message = "fileId is required")
    private Long fileId;

    @NotBlank(message = "fileUsage is required")
    @Size(max = 50, message = "fileUsage must not exceed 50 characters")
    private String fileUsage;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
