package com.bdis.modules.spectrum.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.spectrum.dto.HerbIdentificationQueryRequest;
import com.bdis.modules.spectrum.dto.HerbIdentificationReviewRequest;
import com.bdis.modules.spectrum.dto.HerbIdentifyRequest;
import com.bdis.modules.spectrum.vo.HerbIdentificationPageVO;
import com.bdis.modules.spectrum.vo.HerbIdentificationVO;
import java.util.List;

public interface HerbIdentificationService {

    HerbIdentificationVO identify(Long imageId, HerbIdentifyRequest request);

    HerbIdentificationVO latest(Long imageId);

    List<HerbIdentificationVO> history(Long imageId);

    PageResult<HerbIdentificationPageVO> page(HerbIdentificationQueryRequest request);

    HerbIdentificationVO review(Long id, HerbIdentificationReviewRequest request);
}
