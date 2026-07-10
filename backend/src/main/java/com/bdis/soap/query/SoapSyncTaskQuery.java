package com.bdis.soap.query;

import com.bdis.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SoapSyncTaskQuery extends PageQuery {

    private String resourceType;
    private String status;
    private String direction;
}
