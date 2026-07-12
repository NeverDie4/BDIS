package com.bdis.modules.map.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class HerbBaseVO {
    private Long id;
    private String baseNo;
    private String baseName;
    private String baseType;
    private Long regionId;
    private String regionName;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String contactName;
    private String contactPhone;
    private String description;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
