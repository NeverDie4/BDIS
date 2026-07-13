package com.bdis.modules.herb.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbImageVO {

    private Long id;

    private String imageCode;

    private String imageUrl;

    private String imageName;

    private Long speciesId;

    private Long growthRecordId;

    private String speciesName;

    private String uploadSource;

    private Long collectorId;

    private String uploaderName;

    private Long baseId;

    private String collectPlace;

    private LocalDateTime collectTime;

    private LocalDateTime uploadTime;

    private String imageType;

    private String imageRole;

    private String growthStage;

    private String healthStatus;

    private String processStatus;
}
