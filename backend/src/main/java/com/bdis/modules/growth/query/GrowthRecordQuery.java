package com.bdis.modules.growth.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GrowthRecordQuery extends BaseQuery {
    private Long speciesId;
    private Long distributionId;
    private Long collectorId;
    private String reviewStatus;
}
