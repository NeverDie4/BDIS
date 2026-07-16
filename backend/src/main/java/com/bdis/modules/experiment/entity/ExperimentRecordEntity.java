package com.bdis.modules.experiment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("edu_experiment_record")
public class ExperimentRecordEntity extends BaseEntity {

    private String recordNo;

    private Long courseId;

    private Long projectId;

    private String experimentTitle;

    private String experimentProcess;

    private String experimentResult;

    private Long recorderId;

    private LocalDateTime recordedAt;

    private String archiveStatus;

    private LocalDateTime archivedAt;

    private LocalDateTime submittedAt;

    private Long submittedBy;

    private Long archivedBy;

    private String archiveComment;

    private BigDecimal score;

    private Long gradedBy;

    private LocalDateTime gradedAt;

    private String gradeComment;

    private Long reportFileId;
}
