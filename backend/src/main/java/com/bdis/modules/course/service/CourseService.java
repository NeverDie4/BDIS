package com.bdis.modules.course.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.course.query.CourseQuery;
import com.bdis.modules.course.request.CourseCreateRequest;
import com.bdis.modules.course.request.CourseUpdateRequest;
import com.bdis.modules.course.vo.CourseDetailVO;
import com.bdis.modules.course.vo.CourseListVO;

public interface CourseService {

    PageResult<CourseListVO> page(CourseQuery query);

    CourseDetailVO getDetail(Long id);

    CourseDetailVO create(CourseCreateRequest request);

    CourseDetailVO update(Long id, CourseUpdateRequest request);

    void delete(Long id);

    void publish(Long id);

    void offline(Long id);
}
