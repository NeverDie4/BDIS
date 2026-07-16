package com.bdis.modules.performance.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.performance.dto.PerformanceStandardRequest;
import com.bdis.modules.performance.entity.PerformanceStandardEntity;
import com.bdis.modules.performance.query.PerformanceStandardQuery;

public interface PerformanceStandardService {

    IPage<PerformanceStandardEntity> listStandards(PerformanceStandardQuery query);

    PerformanceStandardEntity createStandard(PerformanceStandardRequest request);

    PerformanceStandardEntity updateStandard(Long standardId, PerformanceStandardRequest request);

    PerformanceStandardEntity createVersion(Long standardId, PerformanceStandardRequest request);

    PerformanceStandardEntity publishStandard(Long standardId);

    PerformanceStandardEntity disableStandard(Long standardId);
}
