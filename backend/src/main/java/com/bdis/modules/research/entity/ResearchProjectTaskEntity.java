package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("research_project_task")
public class ResearchProjectTaskEntity extends BaseEntity {
    private Long projectId;
    private String taskNo;
    private String taskName;
    private String description;
    private Long responsibleUserId;
    private Long baseId;
    private Long sourceRecordId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private LocalDateTime deadlineAt;
    private String taskStatus;
    private Integer sortOrder;
}
