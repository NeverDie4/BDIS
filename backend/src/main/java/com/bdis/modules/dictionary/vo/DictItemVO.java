package com.bdis.modules.dictionary.vo;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DictItemVO {
    private Long id;
    private Long typeId;
    private String itemCode;
    private String itemName;
    private String itemValue;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private List<DictItemVO> children = new ArrayList<>();
}
