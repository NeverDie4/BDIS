package com.bdis.common.core;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseQuery {

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(200)
    private long size = 10;

    private String keyword;

    private Integer status;

    private String sort;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
