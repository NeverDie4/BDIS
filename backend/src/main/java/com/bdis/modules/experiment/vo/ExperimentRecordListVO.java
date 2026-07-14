package com.bdis.modules.experiment.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExperimentRecordListVO {
    private Long id;
    private String recordNo;
    private String sourceType;
    private Long courseId;
    private String courseNo;
    private String courseName;
    private Long projectId;
    private String projectNo;
    private String projectName;
    private String experimentTitle;
    private Long recorderId;
    private String recorderName;
    private LocalDateTime recordedAt;
    private String archiveStatus;
    private BigDecimal score;
    private Long gradedBy;
    private LocalDateTime gradedAt;
    private String gradeComment;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
