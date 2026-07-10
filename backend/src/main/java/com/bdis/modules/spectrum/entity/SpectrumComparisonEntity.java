package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_image_match")
public class SpectrumComparisonEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long imageId;

    private Long atlasId;

    private BigDecimal similarityScore;

    private Integer matchRank;

    private String matchResult;

    private String matchBatchNo;

    private LocalDateTime matchTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
