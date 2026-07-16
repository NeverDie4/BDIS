package com.bdis.modules.training.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingFeedbackListVO {
    private Long id;
    private Long trainingRecordId;
    private Long planId;
    private String planNo;
    private String planName;
    private Long userId;
    private String username;
    private String realName;
    private BigDecimal rating;
    private String feedbackContent;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
