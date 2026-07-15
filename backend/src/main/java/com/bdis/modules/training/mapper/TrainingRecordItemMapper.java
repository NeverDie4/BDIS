package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingRecordItemEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TrainingRecordItemMapper extends BaseMapper<TrainingRecordItemEntity> {
    @Select("SELECT * FROM edu_training_record_item WHERE training_record_id=#{recordId} AND plan_item_id=#{itemId} AND is_deleted=0 LIMIT 1")
    TrainingRecordItemEntity selectActive(@Param("recordId") Long recordId, @Param("itemId") Long itemId);
    @Select("SELECT COUNT(*) FROM edu_training_plan_item WHERE plan_id=#{planId} AND is_required=1 AND status=1 AND is_deleted=0")
    int countRequiredItems(@Param("planId") Long planId);
    @Select("SELECT COUNT(*) FROM edu_training_record_item ri JOIN edu_training_plan_item pi ON pi.id=ri.plan_item_id WHERE ri.training_record_id=#{recordId} AND ri.completed=1 AND pi.is_required=1 AND ri.status=1 AND ri.is_deleted=0")
    int countCompletedRequiredItems(@Param("recordId") Long recordId);
}
