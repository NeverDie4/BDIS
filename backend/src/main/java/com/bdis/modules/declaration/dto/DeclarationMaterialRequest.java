package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationMaterialRequest {

    private Long fileId;

    @NotBlank(message = "材料名称不能为空")
    private String fileName;

    private String fileType;

    private String fileUrl;

    private Long fileSize;

    private Long uploaderId;

    private String remark;
}
