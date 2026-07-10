package com.bdis.audit.query;

import com.bdis.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends PageQuery {

    private Long operatorId;
    private String operationModule;
    private String operationType;
    private String bizType;
    private Long bizId;
}
