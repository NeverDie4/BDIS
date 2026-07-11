package com.bdis.audit.query;

import com.bdis.common.core.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends BaseQuery {

    private Long operatorId;
    private String operationModule;
    private String operationType;
    private String bizType;
    private Long bizId;
}
