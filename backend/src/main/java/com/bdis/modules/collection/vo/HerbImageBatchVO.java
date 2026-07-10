package com.bdis.modules.collection.vo;

import lombok.Data;

@Data
public class HerbImageBatchVO {

    private Long batchId;

    private String batchCode;

    private String batchName;

    private String speciesName;

    private String batchStatus;

    private String bindStatus;

    private String imageRole;

    private Integer isPrimary;
}
