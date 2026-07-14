package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ProjectMemberVO {
    private Long id;
    private Long projectId;
    private Long userId;
    private String username;
    private String realName;
    private String memberRole;
    private String memberStatus;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private String remark;
    private LocalDateTime createdAt;
}
