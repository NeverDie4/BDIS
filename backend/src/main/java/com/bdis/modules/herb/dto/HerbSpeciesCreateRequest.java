package com.bdis.modules.herb.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HerbSpeciesCreateRequest {

    @NotBlank private String herbCode;

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
