package com.bdis.modules.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.experiment.entity.ExperimentRecordVersionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@org.apache.ibatis.annotations.Mapper
public interface ExperimentRecordVersionMapper extends BaseMapper<ExperimentRecordVersionEntity> {
    @Select(
            "SELECT * FROM edu_experiment_record_version WHERE record_id=#{recordId} ORDER BY version_no DESC")
    List<ExperimentRecordVersionEntity> selectByRecordId(@Param("recordId") Long recordId);
}
