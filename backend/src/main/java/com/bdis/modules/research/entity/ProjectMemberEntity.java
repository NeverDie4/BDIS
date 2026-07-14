package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.CreateAuditEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("rel_project_member")
public class ProjectMemberEntity extends CreateAuditEntity {

    private Long projectId;

    private Long userId;

    private String memberRole;

    private String memberStatus;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;
}
