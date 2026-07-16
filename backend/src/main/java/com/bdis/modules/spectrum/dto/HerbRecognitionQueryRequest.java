package com.bdis.modules.spectrum.dto;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class HerbRecognitionQueryRequest {

    private Integer pageNum;

    private Integer pageSize;

    private Long imageId;

    private Long predictedSpeciesId;

    private String predictedName;

    private String recognitionStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
