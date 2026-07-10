package com.bdis.audit.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FileAccessLogVO {

    private Long id;
    private Long fileId;
    private Long operatorId;
    private String operatorName;
    private String accessType;
    private String accessResult;
    private LocalDateTime operationTime;
}
