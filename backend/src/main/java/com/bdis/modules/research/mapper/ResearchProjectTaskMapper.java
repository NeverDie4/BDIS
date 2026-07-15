package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectTaskEntity;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ResearchProjectTaskMapper extends BaseMapper<ResearchProjectTaskEntity> {
    @Select("SELECT * FROM research_project_task WHERE project_id=#{projectId} AND is_deleted=0 ORDER BY sort_order,id")
    List<ResearchProjectTaskEntity> selectByProjectId(Long projectId);
}
