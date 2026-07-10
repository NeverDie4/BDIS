package com.bdis.modules.file.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileResourceVO {

    private Long id;

    private String fileNo;

    private String fileName;

    private String originalFilename;

    private String fileType;

    private String fileFormat;

    private Long fileSize;

    private String fileUrl;

    private String thumbnailUrl;

    private String storageType;

    private LocalDateTime uploadedAt;
}
