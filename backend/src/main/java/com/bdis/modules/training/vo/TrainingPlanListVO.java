package com.bdis.modules.training.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class TrainingPlanListVO {
    private Long id;
    private String planNo;
    private String planName;
    private String planType;
    private Long ownerId;
    private String ownerName;
    private Long courseId;
    private String courseName;
    private Long trainerId;
    private String trainerName;
    private String location;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String publishStatus;
    private Integer status;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
