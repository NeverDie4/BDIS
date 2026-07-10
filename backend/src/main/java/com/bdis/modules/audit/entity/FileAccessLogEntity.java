package com.bdis.modules.audit.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("log_file_access")
public class FileAccessLogEntity extends LogEntity {

    private Long fileId;

    private Long operatorId;

    private String operatorName;

    private String accessType;

    private String accessResult;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime operationTime;
}
