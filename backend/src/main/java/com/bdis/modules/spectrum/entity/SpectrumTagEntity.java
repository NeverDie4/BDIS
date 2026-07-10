package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_atlas_tag")
public class SpectrumTagEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long atlasId;

    private String tagName;

    private String tagType;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
