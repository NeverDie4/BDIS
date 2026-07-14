package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectMapper extends BaseMapper<ResearchProjectEntity> {

    @Select("SELECT * FROM research_project WHERE project_no = #{projectNo} LIMIT 1")
    ResearchProjectEntity selectByProjectNoIncludingDeleted(@Param("projectNo") String projectNo);

    @Select(
            "SELECT COUNT(1) > 0 FROM rel_project_member "
                    + "WHERE project_id = #{projectId} AND user_id = #{userId} "
                    + "AND member_status = 'active'")
    boolean existsActiveMember(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
