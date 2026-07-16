package com.bdis.modules.collection.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.collection.dto.HerbCollectionTaskCreateRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskUpdateRequest;
import com.bdis.modules.collection.vo.HerbCollectionTaskListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import java.util.List;

public interface HerbCollectionTaskService {

    HerbCollectionTaskVO create(HerbCollectionTaskCreateRequest request);

    HerbCollectionTaskVO update(Long id, HerbCollectionTaskUpdateRequest request);

    void delete(Long id);

    HerbCollectionTaskVO getById(Long id);

    PageResult<HerbCollectionTaskVO> page(HerbCollectionTaskQueryRequest request);

    PageResult<HerbCollectionTaskVO> myTasks(HerbCollectionTaskMyQueryRequest request);

    List<HerbCollectionTaskListVO> list(HerbCollectionTaskQueryRequest request);

    HerbCollectionTaskVO publish(Long id);

    HerbCollectionTaskVO start(Long id);

    HerbCollectionTaskVO complete(Long id);

    HerbCollectionTaskVO cancel(Long id);
}
