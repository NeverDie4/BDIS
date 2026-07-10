package com.bdis.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("log_operation")
public class OperationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long operatorId;
    private String operatorName;
    private String operationModule;
    private String operationType;
    private String bizType;
    private Long bizId;
    private String operationResult;
    private String requestMethod;
    private String requestUri;
    private String ipAddress;
    private String userAgent;
    private String errorMessage;
    private LocalDateTime operationTime;
    private LocalDateTime createdAt;
}
