package com.bdis.modules.growth.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GrowthPublicTraceImageVO {

    private String imageUrl;

    private String imageType;

    private String imageRole;

    private LocalDateTime uploadTime;

    private String uploaderName;
}
