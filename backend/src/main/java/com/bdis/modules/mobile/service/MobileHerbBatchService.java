package com.bdis.modules.mobile.service;

import com.bdis.modules.collection.vo.HerbBatchSummaryVO;
import com.bdis.modules.mobile.dto.MobileBatchIdentifyRequest;
import com.bdis.modules.mobile.dto.MobileBatchImageUploadRequest;
import com.bdis.modules.mobile.dto.MobileBatchSubmitRequest;
import com.bdis.modules.mobile.vo.MobileBatchDetailVO;
import com.bdis.modules.mobile.vo.MobileBatchImageUploadResultVO;
import com.bdis.modules.mobile.vo.MobileIdentifyMissingResultVO;
import com.bdis.modules.mobile.vo.MobileImageIdentificationVO;
import org.springframework.web.multipart.MultipartFile;

public interface MobileHerbBatchService {

    MobileBatchDetailVO detail(Long batchId, Long collectorId);

    MobileBatchImageUploadResultVO uploadImage(
            Long batchId, MultipartFile file, MobileBatchImageUploadRequest request);

    MobileBatchImageUploadResultVO identifyImage(
            Long batchId, Long imageId, MobileBatchIdentifyRequest request);

    MobileIdentifyMissingResultVO identifyMissingImages(Long batchId);

    HerbBatchSummaryVO refreshSummary(Long batchId);

    MobileBatchDetailVO submit(Long batchId, MobileBatchSubmitRequest request);

    MobileImageIdentificationVO latestIdentification(Long imageId);
}
