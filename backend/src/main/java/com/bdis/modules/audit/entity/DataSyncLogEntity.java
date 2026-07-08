package com.bdis.modules.audit.entity;

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

    private String sourceSystem;

    private String requestData;

    private String responseData;

    private String syncStatus;

    private String errorMessage;

    private String targetTable;

    private Long targetId;

    private Long operatorId;

    private LocalDateTime operationTime;
}
