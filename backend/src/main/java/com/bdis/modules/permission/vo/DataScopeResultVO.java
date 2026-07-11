package com.bdis.modules.permission.vo;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DataScopeResultVO {

    private String scopeType;

    private Set<Long> organizationIds = new LinkedHashSet<>();

    private Set<Long> departmentIds = new LinkedHashSet<>();

    private Set<String> customRules = new LinkedHashSet<>();

    private boolean selfIncluded;

    private boolean allIncluded;
}
