package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingMaterialEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TrainingMaterialMapper extends BaseMapper<TrainingMaterialEntity> {
    @Select("SELECT * FROM edu_training_material WHERE material_no = #{materialNo} LIMIT 1")
    TrainingMaterialEntity selectByMaterialNoIncludingDeleted(
            @Param("materialNo") String materialNo);

    @Select("SELECT * FROM edu_training_material WHERE id = #{id} LIMIT 1")
    TrainingMaterialEntity selectByIdIncludingDeleted(@Param("id") Long id);

    @Update(
            """
            UPDATE edu_training_material
            SET reuse_count = reuse_count + 1,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = #{id} AND is_deleted = 0
            """)
    int incrementReuseCount(@Param("id") Long id);

    @Update(
            """
            UPDATE edu_training_material
            SET reuse_count = reuse_count - 1,
                updated_at = CURRENT_TIMESTAMP,
                version = version + 1
            WHERE id = #{id} AND is_deleted = 0 AND reuse_count > 0
            """)
    int decrementReuseCount(@Param("id") Long id);

    @Update(
            """
            UPDATE edu_training_material
            SET is_deleted = 1,
                deleted_at = #{deletedAt},
                deleted_by = #{deletedBy},
                updated_at = #{deletedAt},
                updated_by = #{deletedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND reuse_count = 0
              AND version = #{version}
            """)
    int logicalDelete(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("deletedAt") java.time.LocalDateTime deletedAt,
            @Param("deletedBy") Long deletedBy);
}
