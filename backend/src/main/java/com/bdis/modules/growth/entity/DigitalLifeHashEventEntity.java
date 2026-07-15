package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("herb_trace_hash_event")
public class DigitalLifeHashEventEntity extends BaseEntity {

    private Long taskId;

    private Integer hashVersion;

    private String targetType;

    private Long targetId;

    private String eventType;

    private LocalDateTime eventTime;

    private Long actorId;

    private String actorName;

    private String payloadSnapshot;

    private String previousHash;

    private String eventHash;

    private Integer sequence;
}
