package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthAuditHistoryVO {

    private String actionType;

    private String beforeStatus;

    private String afterStatus;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private String comment;

    private LocalDateTime operateTime;
}
