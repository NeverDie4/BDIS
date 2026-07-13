package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingPlanMaterialEntity;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TrainingPlanMaterialMapper extends BaseMapper<TrainingPlanMaterialEntity> {
    @Select("""
            SELECT * FROM rel_training_plan_material
            WHERE plan_id = #{planId} AND material_id = #{materialId}
            LIMIT 1
            """)
    TrainingPlanMaterialEntity selectByPlanAndMaterial(
            @Param("planId") Long planId, @Param("materialId") Long materialId);

    @Select("""
            SELECT r.id AS binding_id, r.plan_id, r.material_id,
                   m.material_no, m.material_name, m.material_type,
                   m.file_id, f.file_name, r.is_required, r.sort_order,
                   m.status AS material_status, m.reuse_count,
                   r.created_at, r.remark
            FROM rel_training_plan_material r
            JOIN edu_training_material m ON m.id = r.material_id
            LEFT JOIN sys_file_resource f ON f.id = m.file_id AND f.is_deleted = 0
            WHERE r.plan_id = #{planId}
            ORDER BY r.sort_order ASC, r.id ASC
            """)
    List<TrainingPlanMaterialVO> selectListVO(@Param("planId") Long planId);

    @Select("""
            SELECT COUNT(1)
            FROM rel_training_plan_material r
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            WHERE r.material_id = #{materialId}
            """)
    Long countActivePlanBindings(@Param("materialId") Long materialId);

    @Select("""
            SELECT COUNT(1)
            FROM rel_training_plan_material r
            JOIN edu_training_material m
              ON m.id = r.material_id AND m.is_deleted = 0 AND m.status = 1
            JOIN sys_file_resource f
              ON f.id = m.file_id AND f.is_deleted = 0 AND f.status = 1
            WHERE r.plan_id = #{planId}
            """)
    Long countValidMaterialsForPublish(@Param("planId") Long planId);
}
