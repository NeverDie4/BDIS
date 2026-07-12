package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationMaterialRequest {
    @NotNull(message = "文件 ID 不能为空")
    private Long fileId;

    private String remark;
}
