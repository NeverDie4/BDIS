package com.bdis.modules.course.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseLearningProgressVO {
    private Long id;
    private Long enrollmentId;
    private Long courseId;
    private Long userId;
    private String itemType;
    private Long itemId;
    private BigDecimal progressValue;
    private Integer progressSeconds;
    private Integer totalSeconds;
    private Integer completed;
    private LocalDateTime firstAccessedAt;
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}
