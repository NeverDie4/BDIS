package com.bdis.modules.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("perf_participant")
public class PerformanceParticipantEntity extends BaseEntity {

    private Long performanceId;

    private Long userId;

    private String participantRole;

    private Integer sortOrder;

    private Integer isPrimary;
}
