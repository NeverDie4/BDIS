package com.bdis.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("log_operation")
public class OperationLogEntity extends LogEntity {

    private String traceId;

    private Long operatorId;

    private String operatorName;

    private String operationModule;

    private String operationType;

    private String operationDesc;

    private String requestMethod;

    @TableField("request_url")
    private String requestUri;

    private String requestParam;

    @TableField("result_status")
    private String operationResult;

    private String errorMessage;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime operationTime;

    private String bizType;

    private Long bizId;
}
