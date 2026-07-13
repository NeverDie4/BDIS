package com.bdis.modules.research.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchProjectListVO {
    private Long id;
    private String projectNo;
    private String projectName;
    private String projectType;
    private Long leaderId;
    private String leaderName;
    private Long speciesId;
    private String speciesName;
    private String projectStatus;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
