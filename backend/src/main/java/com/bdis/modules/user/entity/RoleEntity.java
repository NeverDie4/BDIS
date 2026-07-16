package com.bdis.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_role")
public class RoleEntity extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String roleType;

    private String dataScope;

    private String description;

    private Integer sortOrder;
}
