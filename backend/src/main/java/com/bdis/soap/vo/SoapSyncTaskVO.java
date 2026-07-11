package com.bdis.soap.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class SoapSyncTaskVO {

    private Long id;
    private String taskNo;
    private String resourceType;
    private String serviceName;
    private String methodName;
    private String syncDirection;
    private String syncStatus;
    private Boolean mock;
    private Integer retryCount;
    private LocalDateTime lastSyncAt;
    private String remark;
}
