package com.bdis.modules.experiment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_experiment_record_version")
public class ExperimentRecordVersionEntity extends BasicEntity {
    private Long recordId;
    private Integer versionNo;
    private Long reportFileId;
    private String experimentTitle;
    private String experimentProcess;
    private String experimentResult;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private String status;
}
