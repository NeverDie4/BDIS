package com.bdis.modules.collection.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.collection.dto.HerbBatchCreateRequest;
import com.bdis.modules.collection.dto.HerbBatchQueryRequest;
import com.bdis.modules.collection.dto.HerbBatchUpdateRequest;
import com.bdis.modules.collection.vo.HerbBatchListVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import java.util.List;

public interface HerbBatchService {

    HerbBatchVO create(HerbBatchCreateRequest request);

    HerbBatchVO update(Long id, HerbBatchUpdateRequest request);

    void delete(Long id);

    HerbBatchVO getById(Long id);

    PageResult<HerbBatchVO> page(HerbBatchQueryRequest request);

    List<HerbBatchListVO> list(HerbBatchQueryRequest request);
}
