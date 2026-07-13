package com.bdis.modules.research.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ResearchAchievementCreateRequest {
    @NotBlank @Size(max = 64) private String achievementNo;
    @NotNull private Long projectId;
    @NotBlank @Size(max = 200) private String achievementName;
    @NotBlank @Size(max = 50) private String achievementType;
    @Size(max = 50) private String achievementStage;
    private String description;
    private Long fileId;
    private LocalDateTime publishedAt;
    @Size(max = 500) private String remark;
}
