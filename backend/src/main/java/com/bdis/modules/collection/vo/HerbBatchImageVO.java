package com.bdis.modules.collection.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbBatchImageVO {

    private Long id;

    private Long batchId;

    private Long imageId;

    private String imageCode;

    private String imageUrl;

    private String imageName;

    private String uploadSource;

    private String collectPlace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectTime;

    private String imageType;

    private String growthStage;

    private String healthStatus;

    private Long identificationResultId;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal finalConfidence;

    private String resultSource;

    private String matchResult;

    private Boolean needReview;

    private String reviewStatus;

    private String suggestion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime identifyTime;

    private String imageRole;

    private Integer isPrimary;

    private Integer sortOrder;

    private String bindStatus;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
