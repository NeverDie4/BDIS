package com.bdis.modules.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateDTO {

    private String roleName;

    @Pattern(regexp = "system|business", message = "角色类型只能是 system 或 business")
    private String roleType;

    @Pattern(
            regexp = "all|organization|department|self|custom",
            message = "数据范围只能是 all、organization、department、self、custom")
    private String dataScope;

    private String description;

    private Integer sortOrder;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;
}
