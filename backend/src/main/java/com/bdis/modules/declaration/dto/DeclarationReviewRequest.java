package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationReviewRequest {

    @NotNull(message = "审核人ID不能为空")
    private Long reviewerId;

    @NotBlank(message = "审核动作不能为空")
    private String reviewAction;

    @NotBlank(message = "审核后状态不能为空")
    private String reviewStatus;

    private String reviewComment;

    private String remark;
}
