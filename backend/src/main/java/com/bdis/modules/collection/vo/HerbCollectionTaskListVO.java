package com.bdis.modules.collection.vo;

import lombok.Data;

@Data
public class HerbCollectionTaskListVO {

    private Long id;

    private String taskCode;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long collectorId;

    private String collectorName;

    private String taskStatus;
}
