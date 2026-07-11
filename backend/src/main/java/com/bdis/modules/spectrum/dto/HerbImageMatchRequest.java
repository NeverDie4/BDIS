package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class HerbImageMatchRequest {

    private Integer topK = 5;

    private Long speciesId;

    private Boolean forceRefresh = false;
}
