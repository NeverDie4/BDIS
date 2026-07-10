package com.bdis.file.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("sys_file_resource")
public class FileResourceEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fileNo;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String storagePath;
    private String storageType;
    private String contentType;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime uploadedAt;
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
