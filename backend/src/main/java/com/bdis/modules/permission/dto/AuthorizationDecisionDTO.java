package com.bdis.modules.permission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationDecisionDTO {

    @NotBlank(message = "资源类型不能为空")
    private String resourceType;

    private Long resourceId;

    @NotBlank(message = "操作类型不能为空")
    private String action;

    private String permissionCode;

    private Long ownerUserId;

    private Long organizationId;

    private Long departmentId;
}
