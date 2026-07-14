package com.bdis.modules.course.service;

import com.bdis.modules.course.query.CourseResourceQuery;
import com.bdis.modules.course.request.CourseResourceBindRequest;
import com.bdis.modules.course.vo.CourseResourceVO;
import java.util.List;

public interface CourseResourceService {

    List<CourseResourceVO> listByCourseId(Long courseId, CourseResourceQuery query);

    CourseResourceVO bind(Long courseId, CourseResourceBindRequest request);

    void unbind(Long courseId, Long resourceId);
}
