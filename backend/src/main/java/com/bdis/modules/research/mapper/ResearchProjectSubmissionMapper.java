package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectSubmissionMapper extends BaseMapper<ResearchProjectSubmissionEntity> {
    @Select("SELECT * FROM research_project_submission WHERE id=#{id} AND is_deleted=0 LIMIT 1")
    ResearchProjectSubmissionEntity selectActiveById(Long id);
}
