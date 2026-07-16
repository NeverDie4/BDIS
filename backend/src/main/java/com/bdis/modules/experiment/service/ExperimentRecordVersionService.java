package com.bdis.modules.experiment.service;

import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import com.bdis.modules.experiment.request.ExperimentRecordVersionRequest;
import java.util.List;

public interface ExperimentRecordVersionService {
    ExperimentRecordVersionEntity create(Long recordId, ExperimentRecordVersionRequest request);

    List<ExperimentRecordVersionEntity> list(Long recordId);
}
