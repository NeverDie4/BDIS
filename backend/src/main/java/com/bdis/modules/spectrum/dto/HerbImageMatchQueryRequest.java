package com.bdis.modules.spectrum.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbImageMatchQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long imageId;

    private Long atlasId;

    private Long speciesId;

    private String matchResult;

    private BigDecimal minSimilarity;

    private BigDecimal maxSimilarity;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
