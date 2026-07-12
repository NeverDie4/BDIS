package com.bdis.modules.map.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.map.dto.HerbBaseRequest;
import com.bdis.modules.map.query.HerbBaseQuery;
import com.bdis.modules.map.vo.HerbBaseVO;
import java.util.List;

public interface HerbBaseService {
    PageResult<HerbBaseVO> page(HerbBaseQuery query);

    List<HerbBaseVO> listEnabled();

    HerbBaseVO detail(Long id);

    Long create(HerbBaseRequest request);

    void update(Long id, HerbBaseRequest request);

    void delete(Long id);
}
