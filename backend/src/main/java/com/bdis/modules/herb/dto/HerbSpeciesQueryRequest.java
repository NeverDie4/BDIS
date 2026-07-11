package com.bdis.modules.herb.dto;

import lombok.Data;

@Data
public class HerbSpeciesQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String keyword;

    private String category;

    private Integer status;
}
