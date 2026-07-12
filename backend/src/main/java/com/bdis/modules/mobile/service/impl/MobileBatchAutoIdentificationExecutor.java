package com.bdis.modules.mobile.service.impl;

import com.bdis.common.exception.BusinessException;
import com.bdis.modules.collection.entity.HerbBatchImageEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.mobile.dto.MobileBatchIdentifyRequest;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.service.HerbIdentificationService;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MobileBatchAutoIdentificationExecutor {

    private final HerbBatchImageMapper herbBatchImageMapper;
    private final HerbIdentificationService herbIdentificationService;

    public MobileBatchAutoIdentificationExecutor(
            HerbBatchImageMapper herbBatchImageMapper,
            HerbIdentificationService herbIdentificationService) {
        this.herbBatchImageMapper = herbBatchImageMapper;
        this.herbIdentificationService = herbIdentificationService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void identifyBoundImage(Long batchId, Long imageId, MobileBatchIdentifyRequest request) {
        HerbBatchImageEntity binding = getBoundBinding(batchId, imageId);
        HerbIdentificationVO identification = identify(imageId, request);
        binding.setIdentificationResultId(identification.getId());
        binding.setUpdatedAt(LocalDateTime.now());
        herbBatchImageMapper.updateIdentificationResultById(binding);
    }

    private HerbBatchImageEntity getBoundBinding(Long batchId, Long imageId) {
        HerbBatchImageEntity binding =
                herbBatchImageMapper.selectByBatchIdAndImageId(batchId, imageId);
        if (binding == null) {
            throw new BusinessException("Batch image binding not found");
        }
        return binding;
    }

    private HerbIdentificationVO identify(Long imageId, MobileBatchIdentifyRequest request) {
        HerbIdentifyRequest identifyRequest = new HerbIdentifyRequest();
        if (request != null) {
            identifyRequest.setForceRefresh(request.getForceRefresh());
            identifyRequest.setTopK(request.getTopK());
        }
        return herbIdentificationService.identify(imageId, identifyRequest);
    }
}
