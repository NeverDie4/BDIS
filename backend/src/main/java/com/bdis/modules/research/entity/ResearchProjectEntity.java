package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("research_project")
public class ResearchProjectEntity extends BaseEntity {

    private String projectNo;

    private String projectName;

    private String projectType;

    private Long leaderId;

    private Long speciesId;

    private String description;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private String projectStatus;
}
