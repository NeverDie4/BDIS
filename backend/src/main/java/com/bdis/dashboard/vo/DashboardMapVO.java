package com.bdis.dashboard.vo;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DashboardMapVO {

    private long pointCount;
    private List<Map<String, Object>> districtStatistics;
    private List<Map<String, Object>> points;
}
