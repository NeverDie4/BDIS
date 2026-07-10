package com.bdis.file.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class FileResourceVO {

    private Long id;
    private String fileNo;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String storageType;
    private String contentType;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadedAt;
    private Integer status;
    private String remark;
}
