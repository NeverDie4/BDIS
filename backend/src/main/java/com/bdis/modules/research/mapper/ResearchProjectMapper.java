package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectMapper extends BaseMapper<ResearchProjectEntity> {

    @Select("SELECT * FROM research_project WHERE project_no = #{projectNo} LIMIT 1")
    ResearchProjectEntity selectByProjectNoIncludingDeleted(@Param("projectNo") String projectNo);
}
