package com.bdis.modules.growth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GrowthAuditRequest {
    @NotBlank(message = "审核决定不能为空")
    @Pattern(regexp = "approved|rejected", message = "审核决定只能是 approved 或 rejected")
    private String decision;

    @Size(max = 500)
    private String comment;
}
