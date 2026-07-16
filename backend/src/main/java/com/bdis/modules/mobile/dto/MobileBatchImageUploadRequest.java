package com.bdis.modules.mobile.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class MobileBatchImageUploadRequest {

    private Long collectorId;

    private String collectorName;

    private String collectPlace;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectTime;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private String imageRole;

    private Integer isPrimary;

    private Integer sortOrder;

    private Boolean autoIdentify = true;
}
