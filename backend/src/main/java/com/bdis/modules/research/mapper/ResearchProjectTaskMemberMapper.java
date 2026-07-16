package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectTaskMemberEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ResearchProjectTaskMemberMapper
        extends BaseMapper<ResearchProjectTaskMemberEntity> {
    @Select(
            "SELECT * FROM research_project_task_member WHERE task_id=#{taskId} AND is_deleted=0 ORDER BY id")
    List<ResearchProjectTaskMemberEntity> selectByTaskId(@Param("taskId") Long taskId);

    @Select(
            "SELECT COUNT(*) FROM research_project_task_member WHERE task_id=#{taskId} AND user_id=#{userId} AND status=1 AND is_deleted=0")
    int exists(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
