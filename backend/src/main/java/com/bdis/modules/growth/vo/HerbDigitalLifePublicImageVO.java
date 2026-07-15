package com.bdis.modules.growth.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class HerbDigitalLifePublicImageVO {

    private String imageUrl;

    private String imageType;

    private String imageTypeName;

    private LocalDateTime uploadTime;

    private String uploaderName;

    private Boolean primaryImage;
}
