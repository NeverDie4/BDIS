package com.bdis.modules.course.service;

import com.bdis.modules.course.vo.CourseEnrollmentVO;
import java.util.List;

public interface CourseEnrollmentService {
    CourseEnrollmentVO enroll(Long courseId);

    List<CourseEnrollmentVO> listMine();

    List<CourseEnrollmentVO> listByCourse(Long courseId);
}
