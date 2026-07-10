package com.bdis.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("log_data_change")
public class DataChangeLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String tableName;
    private Long recordId;
    private Long operatorId;
    private String operatorName;
    private String beforeData;
    private String afterData;
    private String changeReason;
    private LocalDateTime operationTime;
    private LocalDateTime createdAt;
}
