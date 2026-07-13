package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_growth_trace_event")
public class GrowthTraceEventEntity extends BaseEntity {

    private Long recordId;

    private String eventType;

    private String eventTitle;

    private String eventContent;

    private String beforeStatus;

    private String afterStatus;

    private Long operatorId;

    private String operatorName;

    private String operatorRole;

    private LocalDateTime eventTime;

    private String metadataJson;
}
