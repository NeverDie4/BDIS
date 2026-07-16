package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingEvaluationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@org.apache.ibatis.annotations.Mapper
public interface TrainingEvaluationMapper extends BaseMapper<TrainingEvaluationEntity> {
    @Select(
            "SELECT * FROM edu_training_record_evaluation WHERE training_record_id=#{recordId} ORDER BY id")
    List<TrainingEvaluationEntity> selectByRecordId(@Param("recordId") Long recordId);

    @Select(
            "SELECT * FROM edu_training_record_evaluation WHERE training_record_id=#{recordId} AND dimension_code=#{dimensionCode} LIMIT 1")
    TrainingEvaluationEntity selectOne(
            @Param("recordId") Long recordId, @Param("dimensionCode") String dimensionCode);
}
