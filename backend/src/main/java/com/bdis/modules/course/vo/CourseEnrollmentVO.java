package com.bdis.modules.course.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CourseEnrollmentVO {
    private Long id;
    private Long courseId;
    private Long userId;
    private String username;
    private String userName;
    private String courseNo;
    private String courseName;
    private Long teacherId;
    private String enrollmentStatus;
    private BigDecimal progress;
    private BigDecimal score;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastAccessedAt;
}
