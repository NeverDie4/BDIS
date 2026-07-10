package com.bdis.modules.spectrum.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class HerbRecognitionVO {

    private Long id;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private Long modelVersionId;

    private Long predictedSpeciesId;

    private String predictedSpeciesName;

    private String predictedName;

    private BigDecimal confidence;

    private String recognitionStatus;

    private Boolean needReview;

    private String recognitionSource;

    private List<HerbRecognitionCandidateVO> candidates;

    private LocalDateTime recognitionTime;

    private String rawResult;
}
