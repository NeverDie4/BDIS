package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_atlas")
public class SpectrumEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long speciesId;

    private String herbName;

    private String atlasCode;

    private String imageUrl;

    private String imageName;

    private String imageType;

    private String growthStage;

    private String medicinalPart;

    private String source;

    private String description;

    private String featureVector;

    private Integer featureDim;

    private String featureModelVersion;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
