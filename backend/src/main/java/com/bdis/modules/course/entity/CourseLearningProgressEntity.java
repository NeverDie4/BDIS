package com.bdis.modules.course.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_course_learning_progress")
public class CourseLearningProgressEntity extends BaseEntity {
    private Long enrollmentId;
    private Long courseId;
    private Long userId;
    private String itemType;
    private Long itemId;
    private java.math.BigDecimal progressValue;
    private Integer progressSeconds;
    private Integer totalSeconds;
    private Integer completed;
    private LocalDateTime firstAccessedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}
