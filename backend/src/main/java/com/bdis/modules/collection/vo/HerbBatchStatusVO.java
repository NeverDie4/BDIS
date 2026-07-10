package com.bdis.modules.collection.vo;

import java.util.List;
import lombok.Data;

@Data
public class HerbBatchStatusVO {

    private Long batchId;

    private String batchCode;

    private String batchName;

    private String batchStatus;

    private Integer imageCount;

    private Integer identifiedCount;

    private Integer reviewedCount;

    private Integer needReviewCount;

    private String finalSpeciesName;

    private String qualityLevel;

    private List<String> allowedActions;
}
