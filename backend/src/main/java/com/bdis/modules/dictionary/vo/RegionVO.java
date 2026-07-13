package com.bdis.modules.dictionary.vo;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class RegionVO {
    private Long id;
    private String regionCode;
    private String regionName;
    private Long parentId;
    private String regionLevel;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private List<RegionVO> children = new ArrayList<>();
}
