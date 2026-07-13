package com.bdis.modules.herb.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HerbImageQueryRequest {

    private Integer pageNum;

    private Integer pageSize;

    private Long speciesId;

    private Long growthRecordId;

    private String keyword;

    private String uploadSource;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private String processStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
