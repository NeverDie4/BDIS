package com.bdis.modules.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("perf_record")
public class PerformanceEntity extends BaseEntity {

    private String performanceNo;

    private Long userId;

    private String performanceTitle;

    private String performanceType;

    private String performanceLevel;

    private LocalDateTime occurredAt;

    private Long standardId;

    private String sourceType;

    private Long sourceId;

    private String sourceNameSnapshot;

    private String standardNoSnapshot;

    private Integer standardVersionSnapshot;

    private String standardNameSnapshot;

    private String standardRuleSnapshot;

    private String identifyStatus;

    private LocalDateTime submittedAt;
}
