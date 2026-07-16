package com.bdis.modules.user.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentQuery extends BaseQuery {

    private Long organizationId;
}
