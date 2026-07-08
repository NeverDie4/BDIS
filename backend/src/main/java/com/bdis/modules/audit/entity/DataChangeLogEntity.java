package com.bdis.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("log_data_change")
public class DataChangeLogEntity extends LogEntity {

    private String tableName;

    private Long recordId;

    private String changeType;

    private String beforeData;

    private String afterData;

    private Long operatorId;

    private LocalDateTime operationTime;
}
