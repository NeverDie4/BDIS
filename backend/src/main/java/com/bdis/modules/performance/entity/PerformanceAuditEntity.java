package com.bdis.modules.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("perf_identification")
public class PerformanceAuditEntity extends CreateAuditEntity {

    private Long performanceId;

    private Long identifierId;

    private String identifyAction;

    private String identifyResult;

    private String identifyComment;

    private LocalDateTime identifiedAt;
}
