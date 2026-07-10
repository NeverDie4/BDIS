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

    private String standardName;

    private String performanceType;

    private String standardDesc;

    private String scoreRule;

    private Integer sortOrder;
}
