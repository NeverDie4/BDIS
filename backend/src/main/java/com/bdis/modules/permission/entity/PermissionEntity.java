package com.bdis.modules.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_permission")
public class PermissionEntity extends BaseEntity {

    private String permissionCode;

    private String permissionName;

    private String permissionType;

    private Long menuId;

    private String apiPath;

    private String requestMethod;

    private String description;
}
