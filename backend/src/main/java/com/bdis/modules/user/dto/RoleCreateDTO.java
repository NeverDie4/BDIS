package com.bdis.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleCreateDTO {

    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @Pattern(regexp = "system|business", message = "角色类型只能是 system 或 business")
    private String roleType;

    @Pattern(
            regexp = "all|organization|department|self|custom",
            message = "数据范围只能是 all、organization、department、self、custom")
    private String dataScope;

    private String description;

    private Integer sortOrder;
}
