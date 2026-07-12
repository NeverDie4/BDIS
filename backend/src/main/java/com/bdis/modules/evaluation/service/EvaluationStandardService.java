package com.bdis.modules.evaluation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.evaluation.dto.EvaluationStandardRequest;
import com.bdis.modules.evaluation.entity.EvaluationIndicatorEntity;
import com.bdis.modules.evaluation.query.EvaluationStandardQuery;

public interface EvaluationStandardService {

    IPage<EvaluationIndicatorEntity> listStandards(EvaluationStandardQuery query);

    EvaluationIndicatorEntity createStandard(EvaluationStandardRequest request);

    EvaluationIndicatorEntity updateStandard(Long standardId, EvaluationStandardRequest request);
}
