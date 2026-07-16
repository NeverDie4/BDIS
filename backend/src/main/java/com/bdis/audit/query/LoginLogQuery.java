package com.bdis.audit.query;

import com.bdis.common.core.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends BaseQuery {

    private Long userId;
    private String username;
    private String loginResult;
}
