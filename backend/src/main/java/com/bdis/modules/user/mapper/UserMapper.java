package com.bdis.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.query.UserQuery;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserMapper extends BaseMapper<UserEntity> {

    Page<UserEntity> selectUserPage(Page<UserEntity> page, @Param("query") UserQuery query);

    @Select(
            "SELECT r.role_code FROM rel_user_role ur "
                    + "JOIN auth_role r ON r.id = ur.role_id "
                    + "WHERE ur.user_id = #{userId} AND r.status = 1 "
                    + "AND r.role_code IN ('TEACHER', 'RESEARCHER') "
                    + "ORDER BY CASE r.role_code WHEN 'TEACHER' THEN 1 ELSE 2 END LIMIT 1")
    String selectResearchLeaderRole(@Param("userId") Long userId);

    @Select(
            "SELECT DISTINCT ur.user_id FROM rel_user_role ur "
                    + "JOIN auth_role r ON r.id = ur.role_id "
                    + "WHERE r.status = 1 AND r.role_code IN ('TEACHER', 'RESEARCHER')")
    List<Long> selectResearchLeaderUserIds();
}
