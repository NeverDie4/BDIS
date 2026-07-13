package com.bdis.modules.training.vo;

import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingFeedbackDetailVO extends TrainingFeedbackListVO {
    private String attendanceStatus;
    private String trainingStatus;
    private BigDecimal progress;
    private BigDecimal score;
    private String remark;
}
