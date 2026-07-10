package com.bdis.modules.collection.service;

import com.bdis.modules.collection.dto.HerbBatchCancelRequest;
import com.bdis.modules.collection.dto.HerbBatchConfirmStatusRequest;
import com.bdis.modules.collection.dto.HerbBatchReopenReviewRequest;
import com.bdis.modules.collection.vo.HerbBatchStatusVO;
import com.bdis.modules.collection.vo.HerbBatchVO;

public interface HerbBatchStatusService {

    HerbBatchVO startCollection(Long batchId);

    HerbBatchVO submit(Long batchId);

    HerbBatchVO startIdentification(Long batchId);

    HerbBatchVO markReviewing(Long batchId);

    HerbBatchVO confirmStatus(Long batchId, HerbBatchConfirmStatusRequest request);

    HerbBatchVO archive(Long batchId);

    HerbBatchVO cancel(Long batchId, HerbBatchCancelRequest request);

    HerbBatchVO reopenReview(Long batchId, HerbBatchReopenReviewRequest request);

    HerbBatchStatusVO status(Long batchId);
}
