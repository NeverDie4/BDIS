package com.bdis.modules.growth.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HerbDigitalLifeImageVO {

    private Long imageId;

    private String imageUrl;

    private String thumbnailUrl;

    private String imageType;

    private String imageTypeName;

    private LocalDateTime uploadTime;

    private String uploaderName;

    private Boolean primaryImage;
}
