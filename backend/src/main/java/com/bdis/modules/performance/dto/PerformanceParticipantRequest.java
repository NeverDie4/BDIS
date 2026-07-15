package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceParticipantRequest {

    @NotNull(message = "参与人不能为空")
    @Positive(message = "参与人 ID 必须大于 0")
    private Long userId;

    @NotBlank(message = "参与角色不能为空")
    private String participantRole;

    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sortOrder;

    private Boolean primary;

    private String remark;
}
