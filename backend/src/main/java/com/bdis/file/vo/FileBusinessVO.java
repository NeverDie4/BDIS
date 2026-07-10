package com.bdis.file.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FileBusinessVO {

    private Long id;
    private Long fileId;
    private String bizType;
    private Long bizId;
    private String fileUsage;
    private LocalDateTime createdAt;
    private String remark;
}
