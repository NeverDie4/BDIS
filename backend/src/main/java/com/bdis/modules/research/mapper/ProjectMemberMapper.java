package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ProjectMemberEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProjectMemberMapper extends BaseMapper<ProjectMemberEntity> {

    @Select("SELECT * FROM rel_project_member WHERE project_id = #{projectId} AND user_id = #{userId} LIMIT 1")
    ProjectMemberEntity selectByProjectIdAndUserId(
            @Param("projectId") Long projectId, @Param("userId") Long userId);
}
