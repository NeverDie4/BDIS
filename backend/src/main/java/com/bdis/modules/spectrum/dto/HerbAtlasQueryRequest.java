package com.bdis.modules.spectrum.dto;

import lombok.Data;

@Data
public class HerbAtlasQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private Long speciesId;

    private String keyword;

    private String imageType;

    private String growthStage;

    private Integer status;
}
