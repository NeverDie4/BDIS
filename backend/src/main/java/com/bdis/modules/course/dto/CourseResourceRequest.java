package com.bdis.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseResourceRequest {
    @NotBlank(message = "资源名称不能为空")
    @Size(max = 150)
    private String resourceName;

    private String resourceType;

    @NotNull(message = "统一文件资源 ID 不能为空")
    private Long fileId;

    private Integer status;
    private String remark;
}
