package com.bdis.modules.collection.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbBatchVO {

    private Long id;

    private String batchCode;

    private String batchName;

    private Long taskId;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String originPlace;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime collectEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime harvestTime;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate productionDate;

    private String batchStatus;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private Long finalSpeciesId;

    private String finalSpeciesName;

    private BigDecimal avgSimilarity;

    private String qualityLevel;

    private BigDecimal qualityScore;

    private String evaluationSummary;

    private String traceCode;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
}
