package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_atlas")
public class SpectrumEntity extends BaseEntity {

    private String atlasNo;

    private Long speciesId;

    private String herbName;

    private String atlasTitle;

    private String imageUrl;

    private String thumbnailUrl;

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String healthStatus;

    private String formType;

    private String colorFeature;

    private String textureFeature;

    private String shapeFeature;

    private String identificationPoints;

    private Long regionId;

    private Long collectorId;

    private LocalDateTime collectedAt;

    private String sourceType;

    private String sourceUrl;

    private String licenseDesc;

    private Integer hasWatermark;

    private String imageQuality;

    private Integer usableForFeature;

    private String featureVector;

    private Integer featureDim;

    private Long featureModelId;

    private String qualityStatus;

    private Long knowledgeEntityId;
}
