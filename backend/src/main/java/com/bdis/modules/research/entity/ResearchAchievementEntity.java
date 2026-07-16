package com.bdis.modules.research.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("research_achievement")
public class ResearchAchievementEntity extends BaseEntity {

    private String achievementNo;

    private Long projectId;

    private Long ownerId;

    private String achievementName;

    private String achievementType;

    private String achievementStage;

    private String achievementStatus;

    private LocalDateTime publishedAt;

    private Long fileId;

    private String fileUrl;

    private String description;
}
