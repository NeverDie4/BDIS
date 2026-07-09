package com.bdis.modules.permission.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationDecisionVO {

    private boolean allowed;

    private String reason;

    private DataScopeResultVO dataScope;
}
