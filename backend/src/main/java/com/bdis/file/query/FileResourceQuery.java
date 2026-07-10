package com.bdis.file.query;

import com.bdis.common.query.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FileResourceQuery extends PageQuery {

    private String fileType;
    private Long uploaderId;
    private String bizType;
    private Long bizId;
    private String businessType;
    private Long businessId;
}
