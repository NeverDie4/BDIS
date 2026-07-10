package com.bdis.common.core;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private Long total;

    private Integer pageNum;

    private Integer pageSize;

    private List<T> records;
}
