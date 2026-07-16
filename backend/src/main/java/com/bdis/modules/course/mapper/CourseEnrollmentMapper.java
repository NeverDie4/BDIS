package com.bdis.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.course.entity.CourseEnrollmentEntity;
import com.bdis.modules.course.vo.CourseEnrollmentVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseEnrollmentMapper extends BaseMapper<CourseEnrollmentEntity> {

    @Select(
            "SELECT * FROM edu_course_enrollment WHERE course_id = #{courseId} AND user_id = #{userId} AND is_deleted = 0 LIMIT 1")
    CourseEnrollmentEntity selectActiveByCourseAndUser(
            @Param("courseId") Long courseId, @Param("userId") Long userId);

    @Select(
            "SELECT ce.id, ce.course_id, ce.user_id, ce.enrollment_status, ce.progress, ce.score, ce.enrolled_at, ce.completed_at, ce.last_accessed_at, u.username, u.real_name AS user_name FROM edu_course_enrollment ce LEFT JOIN sys_user u ON u.id = ce.user_id WHERE ce.course_id = #{courseId} AND ce.is_deleted = 0 ORDER BY ce.enrolled_at DESC")
    List<CourseEnrollmentVO> selectVOByCourseId(@Param("courseId") Long courseId);

    @Select(
            "SELECT ce.id, ce.course_id, ce.user_id, ce.enrollment_status, ce.progress, ce.score, ce.enrolled_at, ce.completed_at, ce.last_accessed_at, c.course_no, c.course_name, c.teacher_id FROM edu_course_enrollment ce JOIN edu_course c ON c.id = ce.course_id LEFT JOIN sys_user u ON u.id = ce.user_id WHERE ce.user_id = #{userId} AND ce.is_deleted = 0 AND ce.enrollment_status <> 'dropped' ORDER BY ce.last_accessed_at DESC, ce.enrolled_at DESC")
    List<CourseEnrollmentVO> selectVOByUserId(@Param("userId") Long userId);

    @Select(
            "SELECT COUNT(1) FROM edu_course_enrollment WHERE course_id=#{courseId} AND user_id=#{userId} AND enrollment_status='completed' AND is_deleted=0")
    int countCompleted(@Param("courseId") Long courseId, @Param("userId") Long userId);
}
