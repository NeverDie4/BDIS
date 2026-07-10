package com.bdis.modules.spectrum.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HerbAtlasUploadRequest {

    @NotNull private Long speciesId;

    private String atlasCode;

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String source;

    private String description;

    private Integer status;

    private String tags;
}
