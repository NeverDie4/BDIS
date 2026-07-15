package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HerbDigitalLifePublicArchiveVO {

    private String traceCode;

    private String taskCode;

    private String taskName;

    private String speciesName;

    private String baseName;

    private String description;

    private Integer stageCount;

    private Integer validStageCount;

    private Integer imageCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String archiveStatus;

    private String qrCodeUrl;

    private List<HerbDigitalLifePublicStageVO> stages = new ArrayList<>();
}
