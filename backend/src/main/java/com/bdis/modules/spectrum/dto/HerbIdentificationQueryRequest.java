package com.bdis.modules.spectrum.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbIdentificationQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long imageId;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private String resultSource;

    private String matchResult;

    private Boolean needReview;

    private String reviewStatus;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
