package com.bdis.modules.herb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_image")
public class HerbImageEntity extends BaseEntity {

    private String imageNo;

    private Long speciesId;

    private Long distributionId;

    private Long growthRecordId;

    private String imageUrl;

    private String thumbnailUrl;

    private String originalFilename;

    private Long fileSize;

    private String fileFormat;

    private String imageType;

    private String imagePurpose;

    private String uploadSource;

    private Long uploaderId;

    private Long regionId;

    private String collectedLocation;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private LocalDateTime collectedAt;

    private String growthStage;

    private String healthStatus;

    private String formType;

    private String featureVector;

    private Integer featureDim;

    private Long featureModelId;

    private String featureModelCode;

    private String featureModelVersion;

    private String processStatus;
}
