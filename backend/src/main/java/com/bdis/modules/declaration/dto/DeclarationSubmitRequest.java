package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationSubmitRequest {

    @NotNull(message = "提交人ID不能为空")
    private Long applicantId;
}
