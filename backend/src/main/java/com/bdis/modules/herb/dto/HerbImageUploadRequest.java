package com.bdis.modules.herb.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HerbImageUploadRequest {

    private Long speciesId;

    private String uploadSource;

    private Long collectorId;

    private Long baseId;

    private Long distributionId;

    private Long growthRecordId;

    private String collectPlace;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    private String imageType;

    private String growthStage;

    private String healthStatus;
}
