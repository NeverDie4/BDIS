package com.bdis.audit.query;

import com.bdis.common.core.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DataSyncLogQuery extends BaseQuery {

    private String syncType;
    private String sourceType;
    private String targetType;
    private String syncStatus;
    private Long taskId;
    private String businessType;
    private Long businessId;
    private String externalNo;
}
