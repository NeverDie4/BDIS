package com.bdis.modules.growth.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HerbDigitalLifeArchiveVO {

    private Long taskId;

    private String taskCode;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private String baseName;

    private String description;

    private String traceCode;

    private Boolean publicVisible;

    private Integer stageCount;

    private Integer validStageCount;

    private Integer imageCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String archiveStatus;

    private List<HerbDigitalLifeStageVO> stages = new ArrayList<>();
}
