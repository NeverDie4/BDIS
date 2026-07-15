package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectTaskMapper extends BaseMapper<ResearchProjectTaskEntity> {
    @Select(
            "SELECT * FROM research_project_task WHERE project_id=#{projectId} AND is_deleted=0 ORDER BY sort_order,id")
    List<ResearchProjectTaskEntity> selectByProjectId(Long projectId);

    @Insert(
            "INSERT INTO rel_research_task_course(task_id,course_id,relation_type,sort_order,status,is_deleted,created_at,created_by) VALUES(#{taskId},#{courseId},'method',#{sortOrder},1,0,NOW(),#{createdBy})")
    int insertCourseRelation(
            @Param("taskId") Long taskId,
            @Param("courseId") Long courseId,
            @Param("sortOrder") int sortOrder,
            @Param("createdBy") Long createdBy);

    @Insert(
            "INSERT INTO rel_research_task_species(task_id,species_id,relation_type,sort_order,status,is_deleted,created_at,created_by) VALUES(#{taskId},#{speciesId},'sample',#{sortOrder},1,0,NOW(),#{createdBy})")
    int insertSpeciesRelation(
            @Param("taskId") Long taskId,
            @Param("speciesId") Long speciesId,
            @Param("sortOrder") int sortOrder,
            @Param("createdBy") Long createdBy);
}
