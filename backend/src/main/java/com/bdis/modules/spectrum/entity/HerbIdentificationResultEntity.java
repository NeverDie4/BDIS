package com.bdis.modules.spectrum.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_identification_result")
public class HerbIdentificationResultEntity extends BaseEntity {

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
}
