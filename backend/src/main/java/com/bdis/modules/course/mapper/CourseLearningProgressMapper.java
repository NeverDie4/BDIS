package com.bdis.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.course.entity.CourseLearningProgressEntity;
import com.bdis.modules.course.vo.CourseLearningProgressVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseLearningProgressMapper extends BaseMapper<CourseLearningProgressEntity> {
    @Select("SELECT * FROM edu_course_learning_progress WHERE enrollment_id=#{enrollmentId} AND item_type=#{itemType} AND item_id=#{itemId} AND is_deleted=0 LIMIT 1")
    CourseLearningProgressEntity selectActive(@Param("enrollmentId") Long enrollmentId, @Param("itemType") String itemType, @Param("itemId") Long itemId);
    @Select("SELECT id,enrollment_id,course_id,user_id,item_type,item_id,progress_value,progress_seconds,total_seconds,completed,first_accessed_at,last_accessed_at,completed_at FROM edu_course_learning_progress WHERE enrollment_id=#{enrollmentId} AND is_deleted=0 ORDER BY item_type,item_id")
    List<CourseLearningProgressVO> selectVOByEnrollment(@Param("enrollmentId") Long enrollmentId);
    @Select("SELECT COUNT(*) FROM edu_experiment_step WHERE course_id=#{courseId} AND status=1 AND is_deleted=0")
    int countActiveSteps(@Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM edu_experiment_step WHERE id=#{itemId} AND course_id=#{courseId} AND status=1 AND is_deleted=0")
    int existsActiveStep(@Param("courseId") Long courseId, @Param("itemId") Long itemId);
    @Select("SELECT COUNT(*) FROM edu_course_resource WHERE course_id=#{courseId} AND status=1 AND is_deleted=0")
    int countActiveResources(@Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM edu_course_resource WHERE id=#{itemId} AND course_id=#{courseId} AND status=1 AND is_deleted=0")
    int existsActiveResource(@Param("courseId") Long courseId, @Param("itemId") Long itemId);
    @Select("SELECT COUNT(*) FROM edu_course_learning_progress WHERE enrollment_id=#{enrollmentId} AND completed=1 AND status=1 AND is_deleted=0")
    int countCompleted(@Param("enrollmentId") Long enrollmentId);

    @Select("SELECT COUNT(*) FROM edu_experiment_step s WHERE s.course_id=#{courseId} AND s.status=1 AND s.is_deleted=0 AND s.sort_order < (SELECT sort_order FROM edu_experiment_step WHERE id=#{stepId} AND course_id=#{courseId} AND is_deleted=0) AND NOT EXISTS (SELECT 1 FROM edu_course_learning_progress p WHERE p.enrollment_id=#{enrollmentId} AND p.item_type='step' AND p.item_id=s.id AND p.completed=1 AND p.status=1 AND p.is_deleted=0)")
    int countIncompletePreviousSteps(@Param("courseId") Long courseId, @Param("stepId") Long stepId, @Param("enrollmentId") Long enrollmentId);

    @Select("SELECT COUNT(*) FROM edu_course_enrollment WHERE course_id=#{courseId} AND is_deleted=0")
    int countEnrollments(@Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM edu_course_learning_progress WHERE course_id=#{courseId} AND item_type='step' AND completed=1 AND status=1 AND is_deleted=0")
    int countCompletedSteps(@Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM edu_experiment_record WHERE course_id=#{courseId} AND is_deleted=0 AND archive_status IN ('submitted','graded','archived')")
    int countSubmittedReports(@Param("courseId") Long courseId);
    @Select("SELECT COUNT(*) FROM edu_experiment_record WHERE course_id=#{courseId} AND is_deleted=0 AND archive_status='submitted'")
    int countUngradedReports(@Param("courseId") Long courseId);
    @Select("SELECT score FROM edu_experiment_record WHERE course_id=#{courseId} AND is_deleted=0 AND archive_status IN ('graded','archived') AND score IS NOT NULL")
    java.util.List<java.math.BigDecimal> selectScores(@Param("courseId") Long courseId);
}
