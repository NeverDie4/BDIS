package com.bdis.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.course.entity.ExperimentStepEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ExperimentStepMapper extends BaseMapper<ExperimentStepEntity> {

    @Select(
            "SELECT * FROM edu_experiment_step "
                    + "WHERE course_id = #{courseId} AND step_no = #{stepNo} LIMIT 1")
    ExperimentStepEntity selectByCourseIdAndStepNoIncludingDeleted(
            @Param("courseId") Long courseId, @Param("stepNo") String stepNo);
}
