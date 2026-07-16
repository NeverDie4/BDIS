package com.bdis.modules.collection.vo;

import java.util.List;
import lombok.Data;

@Data
public class HerbBatchImageBindResultVO {

    private Long batchId;

    private Integer successCount;

    private List<HerbBatchImageVO> images;
}
