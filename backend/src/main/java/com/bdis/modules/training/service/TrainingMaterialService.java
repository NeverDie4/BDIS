package com.bdis.modules.training.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.training.query.TrainingMaterialQuery;
import com.bdis.modules.training.request.TrainingMaterialCreateRequest;
import com.bdis.modules.training.request.TrainingMaterialUpdateRequest;
import com.bdis.modules.training.vo.TrainingMaterialDetailVO;
import com.bdis.modules.training.vo.TrainingMaterialListVO;

public interface TrainingMaterialService {
    PageResult<TrainingMaterialListVO> page(TrainingMaterialQuery query);
    TrainingMaterialDetailVO getDetail(Long id);
    Long create(TrainingMaterialCreateRequest request);
    void update(Long id, TrainingMaterialUpdateRequest request);
    void delete(Long id);
}
