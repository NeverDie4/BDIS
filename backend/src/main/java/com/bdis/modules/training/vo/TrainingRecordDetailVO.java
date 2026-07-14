package com.bdis.modules.training.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingRecordDetailVO extends TrainingRecordListVO {
    private String resultComment;
    private String remark;
    private TrainingFeedbackDetailVO feedback;
}
