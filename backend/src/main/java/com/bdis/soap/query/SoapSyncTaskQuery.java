package com.bdis.soap.query;

import com.bdis.common.core.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SoapSyncTaskQuery extends BaseQuery {

    private String resourceType;
    private String syncStatus;
    private String direction;
}
