package com.bdis.modules.training.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrainingMaterialUpdateRequest {
    @NotBlank @Size(max = 200) private String materialName;
    @NotBlank @Size(max = 50) private String materialType;
    private String description;
    @NotNull @Positive private Long fileId;
    @NotBlank @Size(max = 50) private String sourceType;
    @Positive private Long sourceResourceId;
    @NotNull @Min(0) @Max(1) private Integer status;
    @Size(max = 500) private String remark;
    @NotNull @Min(0) private Integer version;
    @Null(message = "materialNo cannot be changed") private String materialNo;
    @Null(message = "uploaderId cannot be changed") private Long uploaderId;
    @Null(message = "uploadedAt cannot be changed") private java.time.LocalDateTime uploadedAt;
    @Null(message = "reuseCount cannot be changed") private Integer reuseCount;
}
