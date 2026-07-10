package com.bdis.audit.query;

import com.bdis.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileAccessLogQuery extends PageQuery {

    private Long fileId;
    private Long operatorId;
    private String accessType;
}
