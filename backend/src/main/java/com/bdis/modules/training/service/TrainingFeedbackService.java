package com.bdis.modules.training.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.training.query.TrainingFeedbackQuery;
import com.bdis.modules.training.request.TrainingFeedbackCreateRequest;
import com.bdis.modules.training.request.TrainingFeedbackUpdateRequest;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import com.bdis.modules.training.vo.TrainingFeedbackListVO;

public interface TrainingFeedbackService {
    PageResult<TrainingFeedbackListVO> page(TrainingFeedbackQuery query);

    TrainingFeedbackDetailVO getDetail(Long id);

    Long create(TrainingFeedbackCreateRequest request);

    void update(Long id, TrainingFeedbackUpdateRequest request);
}
