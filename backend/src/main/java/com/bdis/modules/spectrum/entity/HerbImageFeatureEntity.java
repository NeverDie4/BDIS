package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_image_feature")
public class HerbImageFeatureEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
