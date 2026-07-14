package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProjectMemberUpdateRequest {
    @NotBlank(message = "memberRole is required")
    @Size(max = 50, message = "memberRole must not exceed 50 characters")
    private String memberRole;

    @Size(max = 500, message = "remark must not exceed 500 characters")
    private String remark;
}
