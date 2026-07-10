package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class HerbIdentifyRequest {

    private Boolean forceRefresh = false;

    private Integer topK;

    private Long speciesId;
}
