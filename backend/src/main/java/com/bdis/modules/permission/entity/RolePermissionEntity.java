package com.bdis.modules.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rel_role_permission")
public class RolePermissionEntity extends CreateAuditEntity {

    private Long roleId;

    private Long permissionId;
}
