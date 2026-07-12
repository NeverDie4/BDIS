package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationRequest {

    private String applicationNo;

    @NotBlank(message = "申报标题不能为空")
    private String applicationTitle;

    private String applicationType;
    private Long applicantId;
    private String reviewStatus;
    private String remark;
}
