package com.bdis.modules.growth.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.growth.dto.GrowthAuditRequest;
import com.bdis.modules.growth.dto.GrowthRecordCreateRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.query.GrowthRecordQuery;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.GrowthTraceEventVO;
import java.util.List;

public interface GrowthRecordService {

    List<GrowthRecordVO> listByPointId(Long pointId);

    GrowthRecordVO createForPoint(Long pointId, GrowthRecordCreateRequest request);

    PageResult<GrowthRecordVO> page(GrowthRecordQuery query);

    GrowthRecordVO detail(Long id);

    GrowthRecordVO create(GrowthRecordUpsertRequest request);

    GrowthRecordVO importFromSoap(
            String externalNo, GrowthRecordUpsertRequest request, String externalCollectorName);

    GrowthRecordVO update(Long id, GrowthRecordUpsertRequest request);

    void delete(Long id);

    GrowthRecordVO submit(Long id);

    GrowthRecordVO audit(Long id, GrowthAuditRequest request);

    GrowthRecordVO archive(Long id);

    List<GrowthTraceEventVO> trace(Long id);
}
