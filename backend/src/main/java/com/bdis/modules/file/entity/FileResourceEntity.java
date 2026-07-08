package com.bdis.modules.file.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_file_resource")
public class FileResourceEntity extends BaseEntity {

    private String fileNo;

    private String fileName;

    private String originalFilename;

    private String fileType;

    private String fileFormat;

    private Long fileSize;

    private String fileUrl;

    private String thumbnailUrl;

    private String storageType;

    private Long uploaderId;

    private LocalDateTime uploadedAt;
}
