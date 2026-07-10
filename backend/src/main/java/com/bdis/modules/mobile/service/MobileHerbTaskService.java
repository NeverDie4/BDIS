package com.bdis.modules.mobile.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.mobile.dto.MobileBatchCreateRequest;
import com.bdis.modules.mobile.dto.MobileTaskQueryRequest;
import com.bdis.modules.mobile.vo.MobileBatchVO;
import com.bdis.modules.mobile.vo.MobileTaskDetailVO;
import com.bdis.modules.mobile.vo.MobileTaskVO;

public interface MobileHerbTaskService {

    PageResult<MobileTaskVO> myTasks(MobileTaskQueryRequest request);

    MobileTaskDetailVO detail(Long taskId, Long collectorId);

    PageResult<MobileBatchVO> batches(
            Long taskId, Long collectorId, String batchStatus, Integer pageNum, Integer pageSize);

    MobileBatchVO createBatch(Long taskId, MobileBatchCreateRequest request);
}
