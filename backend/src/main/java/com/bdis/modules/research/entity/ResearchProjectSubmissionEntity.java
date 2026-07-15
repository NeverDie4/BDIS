package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@TableName("research_project_submission")
public class ResearchProjectSubmissionEntity extends BaseEntity {
    private Long projectId;
    private Long taskId;
    private Long recordId;
    private Long submitterId;
    private String submissionType;
    private String submissionTitle;
    private String content;
    private Long fileId;
    private Integer submissionVersion;
    private String submissionStatus;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
