package com.bdis.modules.permission.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("auth_data_scope")
public class DataScopeEntity extends BaseEntity {

    private Long roleId;

    private String resourceType;

    private String scopeType;

    private Long organizationId;

    private Long departmentId;

    private String customRule;
}
