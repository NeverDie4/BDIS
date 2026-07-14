package com.bdis.modules.training.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TrainingMaterialCreateRequest {
    @NotBlank
    @Size(max = 64)
    private String materialNo;

    @NotBlank
    @Size(max = 200)
    private String materialName;

    @NotBlank
    @Size(max = 50)
    private String materialType;

    private String description;
    @NotNull @Positive private Long fileId;

    @NotBlank
    @Size(max = 50)
    private String sourceType;

    @Positive private Long sourceResourceId;

    @Size(max = 500)
    private String remark;
}
