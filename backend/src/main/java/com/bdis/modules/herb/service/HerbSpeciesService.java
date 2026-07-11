package com.bdis.modules.herb.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.herb.dto.HerbSpeciesCreateRequest;
import com.bdis.modules.herb.dto.HerbSpeciesQueryRequest;
import com.bdis.modules.herb.dto.HerbSpeciesUpdateRequest;
import com.bdis.modules.herb.vo.HerbSpeciesVO;
import java.util.List;

public interface HerbSpeciesService {

    HerbSpeciesVO create(HerbSpeciesCreateRequest request);

    HerbSpeciesVO update(Long id, HerbSpeciesUpdateRequest request);

    void delete(Long id);

    HerbSpeciesVO getById(Long id);

    PageResult<HerbSpeciesVO> page(HerbSpeciesQueryRequest request);

    List<HerbSpeciesVO> listEnabled();
}
