package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthPublicAuditVO {

    private String actionType;
    private String beforeStatus;
    private String afterStatus;
    private String operatorName;
    private String comment;
    private LocalDateTime operateTime;
}
