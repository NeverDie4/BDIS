package com.bdis.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.vo.CourseRelationOptionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface CourseMapper extends BaseMapper<CourseEntity> {

    @Select("SELECT * FROM edu_course " + "WHERE course_no = #{courseNo} LIMIT 1")
    CourseEntity selectByCourseNoIncludingDeleted(@Param("courseNo") String courseNo);

    @Select(
            "SELECT COUNT(1) FROM edu_experiment_record "
                    + "WHERE course_id = #{courseId} AND is_deleted = 0")
    Long countActiveExperimentRecords(@Param("courseId") Long courseId);

    @Select(
            "SELECT s.id, s.herb_no AS code, s.herb_name AS name "
                    + "FROM rel_course_species r JOIN herb_species s ON s.id = r.species_id "
                    + "WHERE r.course_id = #{courseId} AND r.status = 1 AND r.is_deleted = 0 "
                    + "AND s.status = 1 AND s.is_deleted = 0 ORDER BY r.sort_order, r.id")
    List<CourseRelationOptionVO> selectRelatedHerbs(@Param("courseId") Long courseId);

    @Select(
            "SELECT p.id, p.project_no AS code, p.project_name AS name "
                    + "FROM rel_project_course r JOIN research_project p ON p.id = r.project_id "
                    + "WHERE r.course_id = #{courseId} AND r.status = 1 AND r.is_deleted = 0 "
                    + "AND p.status = 1 AND p.is_deleted = 0 ORDER BY r.sort_order, r.id")
    List<CourseRelationOptionVO> selectRelatedProjects(@Param("courseId") Long courseId);

    @Select(
            "SELECT id, herb_no AS code, herb_name AS name FROM herb_species "
                    + "WHERE status = 1 AND is_deleted = 0 ORDER BY herb_name, id")
    List<CourseRelationOptionVO> selectHerbRelationOptions();

    @Select(
            "SELECT p.id, p.project_no AS code, p.project_name AS name FROM research_project p "
                    + "WHERE p.status = 1 AND p.is_deleted = 0 "
                    + "AND (#{applyUserScope} = 0 OR p.leader_id = #{userId} "
                    + "OR EXISTS (SELECT 1 FROM rel_project_member rpm "
                    + "WHERE rpm.project_id = p.id AND rpm.user_id = #{userId} "
                    + "AND rpm.member_status = 'active')) "
                    + "ORDER BY p.project_name, p.id")
    List<CourseRelationOptionVO> selectProjectRelationOptions(
            @Param("userId") Long userId, @Param("applyUserScope") boolean applyUserScope);

    @Update(
            "UPDATE rel_course_species SET status = 0, is_deleted = 1, deleted_at = #{now}, deleted_by = #{userId}, updated_at = #{now}, updated_by = #{userId} "
                    + "WHERE course_id = #{courseId} AND is_deleted = 0")
    int deactivateHerbRelations(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Update(
            "UPDATE rel_course_species SET relation_type = 'material', is_required = 1, "
                    + "sort_order = #{sortOrder}, status = 1, is_deleted = 0, deleted_at = NULL, "
                    + "deleted_by = NULL, updated_at = #{now}, updated_by = #{userId} "
                    + "WHERE course_id = #{courseId} AND species_id = #{speciesId}")
    int restoreHerbRelation(
            @Param("courseId") Long courseId,
            @Param("speciesId") Long speciesId,
            @Param("sortOrder") int sortOrder,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Update(
            "UPDATE rel_project_course SET status = 0, is_deleted = 1, deleted_at = #{now}, deleted_by = #{userId}, updated_at = #{now}, updated_by = #{userId} "
                    + "WHERE course_id = #{courseId} AND is_deleted = 0")
    int deactivateProjectRelations(
            @Param("courseId") Long courseId,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Update(
            "UPDATE rel_project_course SET relation_type = 'foundation', is_primary = 0, "
                    + "sort_order = #{sortOrder}, status = 1, is_deleted = 0, deleted_at = NULL, "
                    + "deleted_by = NULL, updated_at = #{now}, updated_by = #{userId} "
                    + "WHERE project_id = #{projectId} AND course_id = #{courseId}")
    int restoreProjectRelation(
            @Param("courseId") Long courseId,
            @Param("projectId") Long projectId,
            @Param("sortOrder") int sortOrder,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Insert(
            "INSERT INTO rel_course_species (course_id, species_id, relation_type, is_required, sort_order, status, is_deleted, created_at, updated_at, created_by, updated_by, version) "
                    + "VALUES (#{courseId}, #{speciesId}, 'material', 1, #{sortOrder}, 1, 0, #{now}, #{now}, #{userId}, #{userId}, 0)")
    int insertHerbRelation(
            @Param("courseId") Long courseId,
            @Param("speciesId") Long speciesId,
            @Param("sortOrder") int sortOrder,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    @Insert(
            "INSERT INTO rel_project_course (project_id, course_id, relation_type, is_primary, sort_order, status, is_deleted, created_at, updated_at, created_by, updated_by, version) "
                    + "VALUES (#{projectId}, #{courseId}, 'foundation', 0, #{sortOrder}, 1, 0, #{now}, #{now}, #{userId}, #{userId}, 0)")
    int insertProjectRelation(
            @Param("courseId") Long courseId,
            @Param("projectId") Long projectId,
            @Param("sortOrder") int sortOrder,
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);
}
