package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ResearchProjectDetailVO {
    private Long id;
    private String projectNo;
    private String projectName;
    private String projectType;
    private Long leaderId;
    private String leaderName;
    private Long speciesId;
    private String speciesName;
    private String description;
    private String projectStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Long updatedBy;
    private Integer version;
    private List<ProjectMemberVO> members = List.of();
    private List<ProjectMaterialVO> materials = List.of();
    private List<ResearchAchievementListVO> achievements = List.of();
    private ResearchAchievementSummaryVO achievementSummary = new ResearchAchievementSummaryVO();
}
