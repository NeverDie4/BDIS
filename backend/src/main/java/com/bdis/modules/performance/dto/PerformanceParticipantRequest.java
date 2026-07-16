package com.bdis.modules.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PerformanceParticipantRequest {

    @NotNull(message = "参与人不能为空")
    @Positive(message = "参与人 ID 必须大于 0")
    private Long userId;

    @NotBlank(message = "参与角色不能为空")
    @Size(max = 50, message = "参与角色不能超过 50 个字符")
    private String participantRole;

    @PositiveOrZero(message = "排序号不能小于 0")
    private Integer sortOrder;

    private Boolean primary;

    @Size(max = 500, message = "备注不能超过 500 个字符")
    private String remark;
}
