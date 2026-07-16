package com.bdis.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class FileBusinessBindDTO {

    @NotNull private Long fileId;

    @NotBlank private String bizType;

    @NotNull private Long bizId;

    private String fileUsage;

    @PositiveOrZero private Integer sortOrder;

    private String remark;
}
