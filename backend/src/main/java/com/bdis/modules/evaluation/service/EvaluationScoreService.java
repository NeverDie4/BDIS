package com.bdis.modules.evaluation.service;

import com.bdis.modules.evaluation.dto.EvaluationScoreRequest;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;

public interface EvaluationScoreService {

    EvaluationScoreRecordEntity saveScore(EvaluationScoreRequest request);
}
