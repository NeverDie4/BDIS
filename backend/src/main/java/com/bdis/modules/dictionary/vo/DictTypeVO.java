package com.bdis.modules.dictionary.vo;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DictTypeVO {
    private Long id;
    private String typeCode;
    private String typeName;
    private Integer sortOrder;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
