package com.bdis.modules.evaluation.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.bdis.modules.evaluation.dto.EvaluationTaskRequest;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.query.EvaluationTaskQuery;
import com.bdis.modules.evaluation.vo.EvaluationTaskDetailVO;

public interface EvaluationTaskService {

    IPage<EvaluationTaskEntity> listTasks(EvaluationTaskQuery query);

    EvaluationTaskEntity createTask(EvaluationTaskRequest request);

    EvaluationTaskDetailVO getTaskDetail(Long taskId);
}
