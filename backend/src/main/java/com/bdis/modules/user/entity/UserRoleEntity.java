package com.bdis.modules.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rel_user_role")
public class UserRoleEntity extends CreateAuditEntity {

    private Long userId;

    private Long roleId;
}
