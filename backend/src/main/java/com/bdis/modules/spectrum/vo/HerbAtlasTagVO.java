package com.bdis.modules.spectrum.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbAtlasTagVO {

    private Long id;

    private Long atlasId;

    private String tagName;

    private String tagType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
