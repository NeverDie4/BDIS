package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("herb_trace_hash_archive")
public class DigitalLifeHashArchiveEntity extends BaseEntity {

    private Long taskId;

    private Integer hashVersion;

    private String rootHash;

    private Integer eventCount;

    private LocalDateTime generatedTime;

    private Long generatedBy;

    private String integrityStatus;
}
