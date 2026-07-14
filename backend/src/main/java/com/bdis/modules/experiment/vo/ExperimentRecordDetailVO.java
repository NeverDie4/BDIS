package com.bdis.modules.experiment.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class ExperimentRecordDetailVO {
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
    private String experimentProcess;
    private String experimentResult;
    private Long recorderId;
    private String recorderUsername;
    private String recorderName;
    private LocalDateTime recordedAt;
    private String archiveStatus;
    private BigDecimal score;
    private Long gradedBy;
    private LocalDateTime gradedAt;
    private String gradedByName;
    private String gradeComment;
    private LocalDateTime submittedAt;
    private Long submittedBy;
    private String submittedByName;
    private LocalDateTime archivedAt;
    private Long archivedBy;
    private String archivedByName;
    private String archiveComment;
    private Integer status;
    private String remark;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
}
