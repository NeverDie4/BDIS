package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationReviewRequest {

    private Long reviewerId;

    @NotBlank(message = "审核动作不能为空")
    private String reviewAction;

    @NotBlank(message = "审核后状态不能为空")
    private String reviewStatus;

    private String reviewComment;
    private String remark;
}
