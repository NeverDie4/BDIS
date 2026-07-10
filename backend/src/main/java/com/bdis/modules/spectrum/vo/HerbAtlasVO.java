package com.bdis.modules.spectrum.vo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class HerbAtlasVO {

    private Long id;

    private Long speciesId;

    private String herbName;

    private String atlasCode;

    private String imageUrl;

    private String imageName;

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String source;

    private String description;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private List<HerbAtlasTagVO> tags = new ArrayList<>();
}
