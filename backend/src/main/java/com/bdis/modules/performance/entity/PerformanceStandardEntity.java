package com.bdis.modules.performance.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("perf_standard")
public class PerformanceStandardEntity extends BaseEntity {

    private String standardNo;

    private Integer standardVersion;

    private String standardName;

    private String performanceType;

    private String standardDesc;

    private String scoreRule;

    private String levelRule;

    private java.time.LocalDateTime effectiveFrom;

    private java.time.LocalDateTime effectiveTo;

    private Integer materialRequired;

    private Integer minMaterialCount;

    private String lifecycleStatus;

    private java.time.LocalDateTime publishedAt;

    private Long publishedBy;

    private Integer sortOrder;
}
