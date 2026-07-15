package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("research_project_review")
public class ResearchProjectReviewEntity extends BasicEntity {
    private Long projectId;
    private String reviewAction;
    private String fromStatus;
    private String toStatus;
    private String reviewComment;
    private Long operatorId;
    private LocalDateTime operatedAt;
}
