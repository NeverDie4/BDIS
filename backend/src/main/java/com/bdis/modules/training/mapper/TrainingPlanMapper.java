package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface TrainingPlanMapper extends BaseMapper<TrainingPlanEntity> {
    @Select("SELECT * FROM edu_training_plan WHERE plan_no = #{planNo} LIMIT 1")
    TrainingPlanEntity selectByPlanNoIncludingDeleted(@Param("planNo") String planNo);

    @Select("SELECT * FROM edu_training_plan WHERE id = #{id} LIMIT 1")
    TrainingPlanEntity selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM edu_training_record WHERE plan_id = #{planId}")
    Long countRecords(@Param("planId") Long planId);

    @Select("SELECT COUNT(1) FROM rel_training_plan_material WHERE plan_id = #{planId}")
    Long countMaterials(@Param("planId") Long planId);

    @Select(
            "SELECT COUNT(1) FROM edu_training_record "
                    + "WHERE plan_id = #{planId} AND user_id = #{userId}")
    Long countActiveRecordsForUser(@Param("planId") Long planId, @Param("userId") Long userId);

    @Update(
            """
            UPDATE edu_training_plan
            SET publish_status = 'published',
                published_at = #{publishedAt},
                published_by = #{publishedBy},
                updated_at = #{publishedAt},
                updated_by = #{publishedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND publish_status = 'draft'
              AND version = #{version}
            """)
    int publish(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("publishedAt") java.time.LocalDateTime publishedAt,
            @Param("publishedBy") Long publishedBy);

    @Update(
            """
            UPDATE edu_training_plan
            SET publish_status = 'closed',
                updated_at = #{closedAt},
                updated_by = #{closedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND publish_status = 'published'
              AND version = #{version}
            """)
    int close(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("closedAt") java.time.LocalDateTime closedAt,
            @Param("closedBy") Long closedBy);

    @Update(
            """
            UPDATE edu_training_plan
            SET is_deleted = 1,
                deleted_at = #{deletedAt},
                deleted_by = #{deletedBy},
                updated_at = #{deletedAt},
                updated_by = #{deletedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND publish_status = 'draft'
              AND version = #{version}
            """)
    int logicalDelete(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("deletedAt") java.time.LocalDateTime deletedAt,
            @Param("deletedBy") Long deletedBy);
}
