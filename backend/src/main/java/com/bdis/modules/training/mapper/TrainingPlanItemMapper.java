package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingPlanItemEntity;
import org.apache.ibatis.annotations.Select;

public interface TrainingPlanItemMapper extends BaseMapper<TrainingPlanItemEntity> {
    @Select(
            "SELECT * FROM edu_training_plan_item WHERE id=#{id} AND is_deleted=0 AND status=1 LIMIT 1")
    TrainingPlanItemEntity selectActiveById(Long id);
}
