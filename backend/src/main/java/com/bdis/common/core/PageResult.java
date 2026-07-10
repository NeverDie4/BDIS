package com.bdis.common.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import lombok.Getter;

@Getter
public class PageResult<T> {

    private final List<T> records;

    private final long page;

    private final long size;

    private final long total;

    public PageResult(List<T> records, long page, long size, long total) {
        this.records = records;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public PageResult(long total, long page, long size, List<T> records) {
        this(records, page, size, total);
    }

    public static <T> PageResult<T> of(List<T> records, IPage<?> page) {
        return new PageResult<>(records, page.getCurrent(), page.getSize(), page.getTotal());
    }
}
