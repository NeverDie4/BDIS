package com.bdis.modules.soap.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("soap_sync_task")
public class SoapSyncTaskEntity extends BaseEntity {

    private String taskNo;

    private String taskName;

    private String serviceName;

    private String methodName;

    private String syncDirection;

    private String syncStatus;

    private LocalDateTime lastSyncAt;

    private Integer retryCount;
}
