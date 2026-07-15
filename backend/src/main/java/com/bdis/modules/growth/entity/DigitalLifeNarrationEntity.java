package com.bdis.modules.growth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BaseEntity;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("herb_digital_life_narration")
public class DigitalLifeNarrationEntity extends BaseEntity {

    private Long taskId;

    private Long batchId;

    private Long growthRecordId;

    private String narrationType;

    private String narrationText;

    private String narrationSource;

    private String inputSnapshot;

    private String promptVersion;

    private String modelName;

    private Long generatedBy;

    private LocalDateTime generatedTime;
}
