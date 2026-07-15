package com.bdis.modules.collection.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_collection_task")
public class HerbCollectionTaskEntity extends BaseEntity {

    private String taskCode;

    private String taskName;

    private Long speciesId;

    private String speciesName;

    private Long baseId;

    private String baseName;

    private String collectPlace;

    private LocalDateTime plannedStartTime;

    private LocalDateTime plannedEndTime;

    private Long collectorId;

    private String collectorName;

    private String taskStatus;

    private String description;

    private String traceCode;

    private Integer publicVisible;
}
