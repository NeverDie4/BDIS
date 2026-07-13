package com.bdis.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.query.UserQuery;
import org.apache.ibatis.annotations.Param;

public interface UserMapper extends BaseMapper<UserEntity> {

    Page<UserEntity> selectUserPage(Page<UserEntity> page, @Param("query") UserQuery query);
}
