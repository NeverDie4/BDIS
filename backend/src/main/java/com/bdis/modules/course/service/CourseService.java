package com.bdis.modules.course.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.course.dto.CourseRequest;
import com.bdis.modules.course.dto.CourseResourceRequest;
import com.bdis.modules.course.dto.ExperimentStepRequest;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.course.vo.CourseVO;
import com.bdis.modules.course.vo.ExperimentStepVO;

public interface CourseService {
    PageResult<CourseVO> page(CourseQuery query);

    CourseVO detail(Long id);

    Long create(CourseRequest request);

    void update(Long id, CourseRequest request);

    void delete(Long id);

    CourseVO publish(Long id, String status);

    ExperimentStepVO addStep(Long courseId, ExperimentStepRequest request);

    ExperimentStepVO updateStep(Long courseId, Long stepId, ExperimentStepRequest request);

    void deleteStep(Long courseId, Long stepId);

    CourseResourceVO addResource(Long courseId, CourseResourceRequest request);

    void deleteResource(Long courseId, Long resourceId);
}
