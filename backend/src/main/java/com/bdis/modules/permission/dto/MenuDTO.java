package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MenuDTO {

    private Long parentId;

    @NotBlank(message = "菜单编码不能为空")
    private String menuCode;

    @NotBlank(message = "菜单名称不能为空")
    private String menuName;

    private String routePath;

    private String componentPath;

    private String icon;

    private Integer visible;

    private Integer sortOrder;

    private Integer status;
}
