package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class ImageFeatureBatchExtractRequest {

    private Long speciesId;

    private Boolean forceRefresh = false;
}
