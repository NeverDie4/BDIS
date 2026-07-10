package com.bdis.modules.herb.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbSpeciesVO {

    private Long id;

    private String herbCode;

    private String herbName;

    private String latinName;

    private String aliasName;

    private String category;

    private String medicinalPart;

    private String efficacy;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
