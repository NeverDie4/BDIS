package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("research_project_submission_review")
public class ResearchProjectSubmissionReviewEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long submissionId;
    private String reviewAction;
    private String reviewComment;
    private BigDecimal score;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    @Version private Integer version;
}
