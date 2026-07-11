package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class HerbIdentificationReviewRequest {

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private String reviewStatus;

    private String reviewComment;
}
