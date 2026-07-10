package com.bdis.audit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("log_file_access")
public class FileAccessLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long fileId;
    private Long operatorId;
    private String operatorName;
    private String accessType;
    private String accessResult;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime operationTime;
    private LocalDateTime createdAt;
}
