package com.bdis.dashboard.query;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DashboardQuery {

    @Min(1)
    @Max(100)
    private Integer limit = 10;

    private String type;
}
