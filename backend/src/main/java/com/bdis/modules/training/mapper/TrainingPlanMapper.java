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
            "SELECT (SELECT COUNT(*) FROM edu_training_plan_item i WHERE i.plan_id=#{planId} AND i.status=1 AND i.is_deleted=0) + (SELECT COUNT(*) FROM rel_training_plan_course r JOIN edu_course c ON c.id=r.course_id WHERE r.plan_id=#{planId} AND r.status=1 AND r.is_deleted=0 AND c.status=1 AND c.is_deleted=0) + (SELECT COUNT(*) FROM rel_training_plan_project r JOIN research_project p ON p.id=r.project_id WHERE r.plan_id=#{planId} AND r.status=1 AND r.is_deleted=0 AND p.status=1 AND p.is_deleted=0) + (SELECT COUNT(*) FROM rel_training_plan_base r JOIN herb_base b ON b.id=r.base_id WHERE r.plan_id=#{planId} AND r.status=1 AND r.is_deleted=0 AND b.status=1 AND b.is_deleted=0) + (SELECT COUNT(*) FROM rel_training_plan_species r JOIN herb_species s ON s.id=r.species_id WHERE r.plan_id=#{planId} AND r.status=1 AND r.is_deleted=0 AND s.status=1 AND s.is_deleted=0)")
    Long countValidStructuredBindings(@Param("planId") Long planId);

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
