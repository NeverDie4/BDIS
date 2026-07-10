package com.bdis.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_course_resource")
public class CourseResourceEntity extends BaseEntity {

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
}
