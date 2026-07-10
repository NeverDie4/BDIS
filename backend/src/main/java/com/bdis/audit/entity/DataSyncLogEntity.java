package com.bdis.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("log_data_sync")
public class DataSyncLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String syncType;
    private String sourceType;
    private String targetType;
    private Long taskId;
    private Long exchangeId;
    private String businessType;
    private Long businessId;
    private String externalNo;
    private String syncStatus;
    private Integer successCount;
    private Integer failureCount;
    private String failureReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime operationTime;
    private LocalDateTime createdAt;
}
