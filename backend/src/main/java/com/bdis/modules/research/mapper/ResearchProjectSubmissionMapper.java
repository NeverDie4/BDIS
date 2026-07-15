package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectSubmissionMapper
        extends BaseMapper<ResearchProjectSubmissionEntity> {
    @Select("SELECT * FROM research_project_submission WHERE id=#{id} AND is_deleted=0 LIMIT 1")
    ResearchProjectSubmissionEntity selectActiveById(Long id);

    @Select(
            "SELECT * FROM research_project_submission WHERE project_id=#{projectId} AND is_deleted=0 ORDER BY submitted_at DESC, id DESC")
    List<ResearchProjectSubmissionEntity> selectByProjectId(@Param("projectId") Long projectId);
}
