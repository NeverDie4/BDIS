package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("herb_batch")
public class HerbBatchEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String batchCode;

    private String batchName;

    private Long taskId;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String originPlace;

    private LocalDateTime collectStartTime;

    private LocalDateTime collectEndTime;

    private LocalDateTime harvestTime;

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

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
