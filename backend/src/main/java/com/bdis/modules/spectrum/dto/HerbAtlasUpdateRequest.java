package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class HerbAtlasUpdateRequest {

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String source;

    private String description;

    private Integer status;

    private String tags;
}
