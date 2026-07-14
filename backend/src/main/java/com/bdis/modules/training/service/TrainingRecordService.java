package com.bdis.modules.training.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.training.query.TrainingRecordQuery;
import com.bdis.modules.training.request.TrainingParticipantBatchRequest;
import com.bdis.modules.training.request.TrainingRecordCreateRequest;
import com.bdis.modules.training.request.TrainingRecordUpdateRequest;
import com.bdis.modules.training.vo.TrainingParticipantBatchResultVO;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;

public interface TrainingRecordService {
    PageResult<TrainingRecordListVO> page(TrainingRecordQuery query);

    TrainingRecordDetailVO getDetail(Long id);

    Long create(TrainingRecordCreateRequest request);

    TrainingParticipantBatchResultVO batchCreate(
            Long planId, TrainingParticipantBatchRequest request);

    void update(Long id, TrainingRecordUpdateRequest request);

    void remove(Long id);
}
