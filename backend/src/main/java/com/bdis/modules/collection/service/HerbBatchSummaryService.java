package com.bdis.modules.collection.service;

import com.bdis.modules.collection.dto.HerbBatchConfirmRequest;
import com.bdis.modules.collection.vo.HerbBatchIdentificationItemVO;
import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import java.util.List;

public interface HerbBatchSummaryService {

    HerbBatchSummaryVO refreshSummary(Long batchId);

    HerbBatchSummaryVO getSummary(Long batchId);

    List<HerbBatchIdentificationItemVO> listIdentificationItems(Long batchId);

    HerbBatchSummaryVO confirm(Long batchId, HerbBatchConfirmRequest request);
}
