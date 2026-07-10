package com.bdis.modules.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("stat_dashboard_snapshot")
public class DashboardSnapshotEntity extends BasicEntity {

    private LocalDate snapshotDate;

    private Integer herbCount;

    private Integer baseCount;

    private Integer distributionCount;

    private Integer growthRecordCount;

    private Integer courseCount;

    private Integer pendingReviewCount;

    private String dashboardData;
}
