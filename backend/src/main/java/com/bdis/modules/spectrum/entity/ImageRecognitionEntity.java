package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.LogEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_image_recognition")
public class ImageRecognitionEntity extends LogEntity {

    private Long imageId;

    private Long modelId;

    private String modelCode;

    private String modelVersion;

    private Integer rankNo;

    private Long speciesId;

    private String recognizedHerbName;

    private BigDecimal confidence;

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String healthStatus;

    private String formType;

    private String colorFeature;

    private String textureFeature;

    private String shapeFeature;

    private String reason;

    private String suggestion;

    private String rawResponse;

    private Integer isUncertain;
}
