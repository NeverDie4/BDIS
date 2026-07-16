package com.bdis.modules.performance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.performance.dto.PerformanceRequest;
import com.bdis.modules.performance.entity.PerformanceEntity;
import com.bdis.modules.performance.query.PerformanceQuery;
import com.bdis.modules.performance.query.PerformanceStatisticsQuery;
import com.bdis.modules.performance.vo.PerformanceDetailVO;
import com.bdis.modules.performance.vo.PerformanceStatisticsVO;

public interface PerformanceService {

    IPage<PerformanceEntity> listPerformances(PerformanceQuery query);

    PerformanceEntity createPerformance(PerformanceRequest request);

    PerformanceDetailVO getPerformanceDetail(Long performanceId);

    PerformanceEntity updatePerformance(Long performanceId, PerformanceRequest request);

    PerformanceEntity submitPerformance(Long performanceId);

    PerformanceStatisticsVO getStatistics(PerformanceStatisticsQuery query);
}
