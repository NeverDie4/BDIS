package com.bdis.modules.collection.dto;

import java.util.List;
import lombok.Data;

@Data
public class HerbCollectionTaskMyQueryRequest {

    private Long collectorId;

    private String taskStatus;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private List<String> includedStatuses;
}
