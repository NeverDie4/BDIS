package com.bdis.audit.dto;

import lombok.Data;

@Data
public class AuditRecordDTO {

    private String operationModule;
    private String operationType;
    private String bizType;
    private Long bizId;
    private String operationResult = "SUCCESS";
    private String errorMessage;
}
