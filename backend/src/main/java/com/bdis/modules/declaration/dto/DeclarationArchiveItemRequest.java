package com.bdis.modules.declaration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeclarationArchiveItemRequest {

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    @NotNull(message = "来源记录ID不能为空")
    private Long sourceId;

    private String itemName;

    private String itemDesc;

    private Integer sortOrder;

    private String remark;
}
