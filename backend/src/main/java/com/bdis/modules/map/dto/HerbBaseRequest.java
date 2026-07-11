package com.bdis.modules.map.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HerbBaseRequest {
    @NotBlank(message = "基地编号不能为空")
    @Size(max = 64)
    private String baseNo;

    @NotBlank(message = "基地名称不能为空")
    @Size(max = 150)
    private String baseName;

    private String baseType;
    private Long regionId;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String contactName;
    private String contactPhone;
    private String description;
    private Integer status;
    private String remark;
}
