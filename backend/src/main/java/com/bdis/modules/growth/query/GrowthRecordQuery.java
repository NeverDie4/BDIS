package com.bdis.modules.growth.query;

import com.bdis.common.core.BaseQuery;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.StringUtils;

@Getter
@Setter
public class GrowthRecordQuery extends BaseQuery {
    private Long pageNum;
    private Long pageSize;
    private Long herbId;
    private Long speciesId;
    private Long taskId;
    private Long batchId;
    private Long baseId;
    private Long distributionId;
    private Long collectorId;
    private String reviewStatus;

    public void setStatus(String status) {
        if (StringUtils.hasText(status)) {
            this.reviewStatus = status;
        }
    }

    public void setAuditStatus(String auditStatus) {
        if (StringUtils.hasText(auditStatus)) {
            this.reviewStatus = auditStatus;
        }
    }
}
