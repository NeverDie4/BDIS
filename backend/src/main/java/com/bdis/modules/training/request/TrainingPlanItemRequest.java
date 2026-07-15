package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data public class TrainingPlanItemRequest { @NotBlank @Size(max=50) private String itemType; @NotBlank @Size(max=200) private String itemTitle; private String description; private Long courseId; private Long projectId; private Long baseId; private Long speciesId; private Long fileId; private Integer isRequired=1; private BigDecimal completionWeight; private Integer sortOrder=0; }
