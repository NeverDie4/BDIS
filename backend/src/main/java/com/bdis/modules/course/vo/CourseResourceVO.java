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
    private String fileUrl;
    private Long fileSize;
    private String fileFormat;
    private Long uploaderId;
    private LocalDateTime uploadedAt;
    private Integer downloadCount;
    private Integer status;
    private String remark;
}
