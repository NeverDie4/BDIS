package com.bdis.soap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("soap_sync_task")
public class SoapSyncTaskEntity {

    @TableId(type = IdType.AUTO)
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
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;
    private String remark;
    private Integer version;
}
