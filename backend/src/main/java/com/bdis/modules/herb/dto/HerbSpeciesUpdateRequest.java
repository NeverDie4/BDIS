package com.bdis.modules.herb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HerbSpeciesUpdateRequest {

    @NotBlank private String herbName;

    private String latinName;

    private String aliasName;

    private String category;

    private String medicinalPart;

    private String efficacy;

    private String description;

    private String coverImageUrl;

    private Integer status;
}
