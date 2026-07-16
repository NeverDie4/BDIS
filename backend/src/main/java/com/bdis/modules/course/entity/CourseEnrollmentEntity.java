package com.bdis.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_course_enrollment")
public class CourseEnrollmentEntity extends BaseEntity {
    private Long courseId;
    private Long userId;
    private String enrollmentStatus;
    private BigDecimal progress;
    private BigDecimal score;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
}
