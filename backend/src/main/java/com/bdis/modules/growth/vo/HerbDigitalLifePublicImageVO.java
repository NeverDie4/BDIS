package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

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
