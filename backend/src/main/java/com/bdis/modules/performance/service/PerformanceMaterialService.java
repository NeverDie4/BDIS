package com.bdis.modules.performance.service;

import com.bdis.modules.performance.dto.PerformanceMaterialRequest;
import com.bdis.modules.performance.vo.PerformanceMaterialVO;
import java.util.List;

public interface PerformanceMaterialService {

    PerformanceMaterialVO addMaterial(Long performanceId, PerformanceMaterialRequest request);

    List<PerformanceMaterialVO> listMaterials(Long performanceId);
}
