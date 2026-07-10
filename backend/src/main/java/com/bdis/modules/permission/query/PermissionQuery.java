package com.bdis.modules.permission.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionQuery extends BaseQuery {

    private String permissionType;

    private Long menuId;
}
