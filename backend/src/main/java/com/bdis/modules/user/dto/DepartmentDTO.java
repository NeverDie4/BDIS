package com.bdis.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentDTO {

    @NotBlank(message = "部门编号不能为空")
    private String departmentNo;

    @NotBlank(message = "部门名称不能为空")
    private String departmentName;

    @NotNull(message = "所属机构不能为空")
    private Long organizationId;

    private Long parentId;

    private Integer sortOrder;

    private Integer status;
}
