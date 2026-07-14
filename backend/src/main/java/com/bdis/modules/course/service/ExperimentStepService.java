package com.bdis.modules.course.service;

import com.bdis.modules.course.request.ExperimentStepCreateRequest;
import com.bdis.modules.course.request.ExperimentStepUpdateRequest;
import com.bdis.modules.course.vo.ExperimentStepVO;
import java.util.List;

public interface ExperimentStepService {

    List<ExperimentStepVO> listByCourseId(Long courseId);

    ExperimentStepVO create(Long courseId, ExperimentStepCreateRequest request);

    ExperimentStepVO update(Long courseId, Long stepId, ExperimentStepUpdateRequest request);

    void delete(Long courseId, Long stepId);
}
