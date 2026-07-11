package com.bdis.modules.map.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HerbBaseQuery extends BaseQuery {
    private Long regionId;
    private String baseType;
}
