package com.bdis.modules.training.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingPlanDetailVO extends TrainingPlanListVO {
    private String description;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private String publishedByName;
    private Long participantCount;
    private List<TrainingPlanMaterialVO> materials;
    private Long materialCount;
    private Long requiredMaterialCount;
    private String remark;
    private Long createdBy;
    private Long updatedBy;
}
