package com.bdis.modules.evaluation.service;

import com.bdis.modules.evaluation.dto.EvaluationConfirmationRequest;
import com.bdis.modules.evaluation.entity.EvaluationResultEntity;

public interface EvaluationResultService {

    EvaluationResultEntity confirmByScoreRecord(Long recordId, EvaluationConfirmationRequest request);
}
