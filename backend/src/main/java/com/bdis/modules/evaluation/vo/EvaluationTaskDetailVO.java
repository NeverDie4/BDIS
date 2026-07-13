package com.bdis.modules.evaluation.vo;

import com.bdis.modules.evaluation.entity.EvaluationResultEntity;
import com.bdis.modules.evaluation.entity.EvaluationScoreRecordEntity;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EvaluationTaskDetailVO {

    private EvaluationTaskEntity task;

    private List<EvaluationScoreRecordEntity> scores;

    private EvaluationResultEntity result;
}
