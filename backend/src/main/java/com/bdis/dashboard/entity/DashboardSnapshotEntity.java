package com.bdis.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("stat_dashboard_snapshot")
public class DashboardSnapshotEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate snapshotDate;
    private Integer herbCount;
    private Integer baseCount;
    private Integer distributionCount;
    private Integer growthRecordCount;
    private Integer pendingTaskCount;
    private String dashboardData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;
}
