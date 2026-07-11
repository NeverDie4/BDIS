package com.bdis.dashboard.service;

import com.bdis.dashboard.query.DashboardQuery;
import com.bdis.dashboard.vo.DashboardMapVO;
import com.bdis.dashboard.vo.DashboardRecentGrowthRecordVO;
import com.bdis.dashboard.vo.DashboardSummaryVO;
import com.bdis.dashboard.vo.DashboardTodoVO;
import java.util.List;

public interface DashboardService {

    DashboardSummaryVO summary();

    List<DashboardRecentGrowthRecordVO> recentGrowthRecords(DashboardQuery query);

    List<DashboardTodoVO> pendingTasks(DashboardQuery query);

    DashboardMapVO mapOverview();
}
