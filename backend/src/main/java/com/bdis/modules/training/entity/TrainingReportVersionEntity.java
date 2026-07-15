package com.bdis.modules.training.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.bdis.common.core.BasicEntity;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("edu_training_record_report_version")
public class TrainingReportVersionEntity extends BasicEntity {
    private Long trainingRecordId;
    private Integer versionNo;
    private Long fileId;
    private String content;
    private String reportStatus;
    private Long submittedBy;
    private LocalDateTime submittedAt;
}
