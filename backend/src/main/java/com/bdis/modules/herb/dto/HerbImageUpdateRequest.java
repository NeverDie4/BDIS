package com.bdis.modules.herb.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HerbImageUpdateRequest {

    private Long speciesId;

    private Long collectorId;

    private Long baseId;

    private String collectPlace;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private String processStatus;
}
