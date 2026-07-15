package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Data;

@Data @TableName("research_project_task_member")
public class ResearchProjectTaskMemberEntity extends BaseEntity {
    private Long taskId; private Long userId; private String memberRole; private Long assignedBy; private LocalDateTime assignedAt;
}
