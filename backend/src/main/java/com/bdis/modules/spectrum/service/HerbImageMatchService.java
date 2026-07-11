package com.bdis.modules.spectrum.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.spectrum.dto.HerbImageMatchQueryRequest;
import com.bdis.modules.spectrum.dto.HerbImageMatchRequest;
import com.bdis.modules.spectrum.vo.HerbImageMatchPageVO;
import com.bdis.modules.spectrum.vo.HerbImageMatchVO;
import java.util.List;

public interface HerbImageMatchService {

    HerbImageMatchVO match(Long imageId, HerbImageMatchRequest request);

    HerbImageMatchVO latest(Long imageId);

    List<HerbImageMatchPageVO> listByImageId(Long imageId);

    PageResult<HerbImageMatchPageVO> page(HerbImageMatchQueryRequest request);
}
