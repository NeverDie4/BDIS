package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_ai_model_version")
public class AiModelVersionEntity extends BaseEntity {

    private String modelCode;

    private String modelName;

    private String modelType;

    private String modelVersion;

    private String provider;

    private String apiEndpoint;

    private Integer featureDim;

    private Integer isDefault;
}
