package com.bdis.audit.query;

import com.bdis.common.core.BaseQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileAccessLogQuery extends BaseQuery {

    private Long fileId;
    private Long operatorId;
    private String accessType;
}
