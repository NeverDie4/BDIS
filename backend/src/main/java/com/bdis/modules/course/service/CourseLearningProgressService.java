package com.bdis.modules.course.service;

import com.bdis.modules.course.request.CourseLearningProgressRequest;
import com.bdis.modules.course.vo.CourseLearningProgressVO;
import com.bdis.modules.course.vo.CourseLearningSummaryVO;
import java.util.List;

public interface CourseLearningProgressService {
    List<CourseLearningProgressVO> list(Long courseId);

    CourseLearningProgressVO save(Long courseId, CourseLearningProgressRequest request);

    CourseLearningSummaryVO summary(Long courseId);
}
