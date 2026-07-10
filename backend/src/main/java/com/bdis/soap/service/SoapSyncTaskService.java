package com.bdis.soap.service;

import com.bdis.common.response.PageResult;
import com.bdis.soap.dto.SoapRetryDTO;
import com.bdis.soap.dto.SoapSyncTaskDTO;
import com.bdis.soap.query.SoapSyncTaskQuery;
import com.bdis.soap.vo.SoapExchangeRecordVO;
import com.bdis.soap.vo.SoapSyncTaskVO;

public interface SoapSyncTaskService {

    SoapExchangeRecordVO createAndExecute(SoapSyncTaskDTO dto);

    PageResult<SoapSyncTaskVO> page(SoapSyncTaskQuery query);

    SoapExchangeRecordVO detail(Long jobId);

    SoapExchangeRecordVO retry(Long jobId, SoapRetryDTO dto);
}
