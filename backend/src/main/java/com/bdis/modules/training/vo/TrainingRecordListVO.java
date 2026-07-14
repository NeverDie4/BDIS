package com.bdis.modules.training.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingRecordListVO {
    private Long id;
    private Long planId;
    private String planNo;
    private String planName;
    private Long userId;
    private String username;
    private String realName;
    private Long courseId;
    private BigDecimal progress;
    private String trainingStatus;
    private String attendanceStatus;
    private BigDecimal score;
    private LocalDateTime startedAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
