package com.bdis.audit.query;

import com.bdis.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginLogQuery extends PageQuery {

    private Long userId;
    private String username;
    private String loginResult;
}
