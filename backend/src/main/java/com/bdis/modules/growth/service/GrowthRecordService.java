package com.bdis.modules.growth.service;

import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import java.util.List;

public interface GrowthRecordService {

    List<GrowthRecordVO> listByPointId(Long pointId);

    GrowthRecordVO createForPoint(Long pointId, GrowthRecordCreateRequest request);
}
