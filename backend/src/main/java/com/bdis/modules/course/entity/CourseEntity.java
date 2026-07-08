package com.bdis.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_course")
public class CourseEntity extends BaseEntity {

    private String courseNo;

    private String courseName;

    private String courseType;

    private Long teacherId;

    private String description;

    private String videoUrl;

    private String publishStatus;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
}
