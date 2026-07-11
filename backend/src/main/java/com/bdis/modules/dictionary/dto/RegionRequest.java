package com.bdis.modules.dictionary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class RegionRequest {

    @NotBlank(message = "区域编码不能为空")
    @Size(max = 64)
    private String regionCode;

    @NotBlank(message = "区域名称不能为空")
    @Size(max = 100)
    private String regionName;

    private Long parentId;
    private String regionLevel;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
