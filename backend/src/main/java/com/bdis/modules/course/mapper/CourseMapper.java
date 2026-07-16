package com.bdis.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.course.entity.CourseEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CourseMapper extends BaseMapper<CourseEntity> {

    @Select("SELECT * FROM edu_course " + "WHERE course_no = #{courseNo} LIMIT 1")
    CourseEntity selectByCourseNoIncludingDeleted(@Param("courseNo") String courseNo);

    @Select(
            "SELECT COUNT(1) FROM edu_experiment_record "
                    + "WHERE course_id = #{courseId} AND is_deleted = 0")
    Long countActiveExperimentRecords(@Param("courseId") Long courseId);
}
