package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_image_feature")
public class HerbImageFeatureEntity extends BaseEntity {

    private Long imageId;

    private Long speciesId;

    private String featureCode;

    private String featureVector;

    private Integer featureDimension;

    private String featureModel;

    private String featureVersion;

    private String extractStatus;

    private LocalDateTime extractTime;

    private String errorMessage;
}
