package com.bdis.modules.user.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.user.dto.AdminPasswordResetDTO;
import com.bdis.modules.user.dto.RoleAssignDTO;
import com.bdis.modules.user.dto.UserCreateDTO;
import com.bdis.modules.user.dto.UserUpdateDTO;
import com.bdis.modules.user.query.UserQuery;
import com.bdis.modules.user.vo.UserVO;

public interface UserService {

    PageResult<UserVO> page(UserQuery query);

    UserVO detail(Long id);

    Long create(UserCreateDTO dto);

    void update(Long id, UserUpdateDTO dto);

    void patch(Long id, UserUpdateDTO dto);

    void delete(Long id);

    void assignRoles(Long id, RoleAssignDTO dto);

    void resetPassword(Long id, AdminPasswordResetDTO dto);
}
