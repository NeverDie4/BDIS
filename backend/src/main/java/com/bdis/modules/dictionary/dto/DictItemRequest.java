package com.bdis.modules.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictItemRequest {

    @NotBlank(message = "字典项编码不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "字典项编码格式不正确")
    @Size(max = 64)
    private String itemCode;

    @NotBlank(message = "字典项名称不能为空")
    @Size(max = 100)
    private String itemName;

    @Size(max = 100)
    private String itemValue;

    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
