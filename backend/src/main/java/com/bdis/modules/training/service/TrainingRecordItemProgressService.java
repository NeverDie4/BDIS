package com.bdis.modules.training.service;

import com.bdis.modules.training.entity.TrainingRecordItemEntity;
import com.bdis.modules.training.request.TrainingRecordItemProgressRequest;

public interface TrainingRecordItemProgressService {
    TrainingRecordItemEntity save(Long recordId, Long itemId, TrainingRecordItemProgressRequest request);
}
