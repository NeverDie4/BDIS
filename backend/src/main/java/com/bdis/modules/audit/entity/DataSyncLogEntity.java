package com.bdis.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("log_data_sync")
public class DataSyncLogEntity extends LogEntity {

    private String traceId;

    private String syncType;

    @TableField("source_system")
    private String sourceType;

    private String requestData;

    private String responseData;

    private String syncStatus;

    @TableField("error_message")
    private String failureReason;

    @TableField("target_table")
    private String targetType;

    private Long targetId;

    private Long operatorId;

    private String operatorName;

    private Long taskId;

    private Long exchangeId;

    @TableField("biz_type")
    private String businessType;

    @TableField("biz_id")
    private Long businessId;

    private String externalNo;

    private Integer successCount;

    private Integer failureCount;

    private LocalDateTime operationTime;
}
