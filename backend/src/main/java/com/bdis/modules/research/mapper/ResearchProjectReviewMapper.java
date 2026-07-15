package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectReviewEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectReviewMapper extends BaseMapper<ResearchProjectReviewEntity> {
    @Select("SELECT * FROM research_project_review WHERE project_id=#{projectId} ORDER BY operated_at DESC, id DESC")
    List<ResearchProjectReviewEntity> selectByProjectId(@Param("projectId") Long projectId);
}
