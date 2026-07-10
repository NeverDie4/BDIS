package com.bdis.common.response;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.Data;

@Data
public class PageResult<T> {

    private List<T> records;
    private long pageNum;
    private long pageSize;
    private long total;

    public static <T> PageResult<T> of(List<T> records, Page<?> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(records);
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        return result;
    }
}
