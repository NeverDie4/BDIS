package com.bdis.modules.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ExperimentStepRequest {
    @NotBlank(message = "步骤编号不能为空")
    @Size(max = 50)
    private String stepNo;

    @NotBlank(message = "步骤标题不能为空")
    @Size(max = 200)
    private String stepTitle;

    private String stepContent;
    private String expectedResult;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
