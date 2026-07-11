package com.bdis.modules.collection.vo;

import lombok.Data;

@Data
public class HerbBatchListVO {

    private Long id;

    private String batchCode;

    private String batchName;

    private Long speciesId;

    private String speciesName;

    private String batchStatus;
}
