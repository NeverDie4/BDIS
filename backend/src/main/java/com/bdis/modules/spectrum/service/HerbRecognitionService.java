package com.bdis.modules.spectrum.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.spectrum.dto.HerbRecognitionQueryRequest;
import com.bdis.modules.spectrum.vo.HerbRecognitionVO;
import java.util.List;

public interface HerbRecognitionService {

    HerbRecognitionVO recognize(Long imageId);

    HerbRecognitionVO recognizeByDoubao(Long imageId);

    HerbRecognitionVO latest(Long imageId);

    HerbRecognitionVO getById(Long id);

    List<HerbRecognitionVO> history(Long imageId);

    PageResult<HerbRecognitionVO> page(HerbRecognitionQueryRequest request);
}
