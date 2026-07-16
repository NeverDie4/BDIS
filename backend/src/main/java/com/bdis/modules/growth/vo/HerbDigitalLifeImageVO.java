package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

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
