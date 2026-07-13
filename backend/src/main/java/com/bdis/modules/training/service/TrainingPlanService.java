package com.bdis.modules.training.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.training.query.TrainingPlanQuery;
import com.bdis.modules.training.request.TrainingPlanCreateRequest;
import com.bdis.modules.training.request.TrainingPlanCloseRequest;
import com.bdis.modules.training.request.TrainingPlanPublishRequest;
import com.bdis.modules.training.request.TrainingPlanUpdateRequest;
import com.bdis.modules.training.vo.TrainingPlanDetailVO;
import com.bdis.modules.training.vo.TrainingPlanListVO;

public interface TrainingPlanService {
    PageResult<TrainingPlanListVO> page(TrainingPlanQuery query);
    TrainingPlanDetailVO getDetail(Long id);
    Long create(TrainingPlanCreateRequest request);
    void update(Long id, TrainingPlanUpdateRequest request);
    void delete(Long id);
    void publish(Long id, TrainingPlanPublishRequest request);
    void close(Long id, TrainingPlanCloseRequest request);
}
