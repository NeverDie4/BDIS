package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_atlas_tag")
public class SpectrumTagEntity extends BaseEntity {

    private Long atlasId;

    private String tagName;

    private String tagType;

    private String tagSource;

    private Integer sortOrder;
}
