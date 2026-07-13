package com.bdis.modules.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictTypeRequest {

    @NotBlank(message = "字典类型编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型编码必须为小写英文下划线格式")
    @Size(max = 64)
    private String typeCode;

    @NotBlank(message = "字典类型名称不能为空")
    @Size(max = 100)
    private String typeName;

    private Integer sortOrder;
    private Integer status;
    private String remark;
}
