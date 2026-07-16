package com.bdis.modules.course.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseResourceVO {

    private Long id;
    private Long courseId;
    private String resourceName;
    private String resourceType;
    private Long fileId;
    private String fileName;
    private String originalFilename;
    private String fileType;
    private String fileFormat;
    private Long fileSize;
    private String fileUrl;
    private String thumbnailUrl;
    private String contentType;
    private Long uploaderId;
    private LocalDateTime uploadedAt;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String remark;
}
