package com.bdis.audit.service;

import com.bdis.audit.dto.DataSyncRecordDTO;
import com.bdis.audit.query.DataSyncLogQuery;
import com.bdis.audit.vo.DataSyncLogVO;
import com.bdis.common.response.PageResult;

public interface DataSyncLogService {

    void record(DataSyncRecordDTO dto);

    PageResult<DataSyncLogVO> page(DataSyncLogQuery query);
}
