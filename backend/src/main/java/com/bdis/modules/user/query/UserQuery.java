package com.bdis.modules.user.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserQuery extends BaseQuery {

    private Long roleId;

    private String userType;

    private Long organizationId;

    private Long departmentId;
}
