package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_identification_result")
public class HerbIdentificationResultEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long imageId;

    private Long bestMatchId;

    private Long recognitionId;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Integer needReview;

    private String reviewStatus;

    private String suggestion;

    private String reviewComment;

    private Long reviewerId;

    private String reviewerName;

    private LocalDateTime reviewTime;

    private String rawSummary;

    private LocalDateTime identifyTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
