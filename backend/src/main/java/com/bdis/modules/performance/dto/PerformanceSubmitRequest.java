package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceSubmitRequest {

    @NotNull(message = "提交人ID不能为空")
    private Long userId;
}
