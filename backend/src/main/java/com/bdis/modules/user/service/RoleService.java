package com.bdis.modules.user.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.user.dto.RoleCreateDTO;
import com.bdis.modules.user.dto.RolePermissionAssignDTO;
import com.bdis.modules.user.dto.RoleUpdateDTO;
import com.bdis.modules.user.query.RoleQuery;
import com.bdis.modules.user.vo.RoleVO;

public interface RoleService {

    PageResult<RoleVO> page(RoleQuery query);

    RoleVO detail(Long id);

    Long create(RoleCreateDTO dto);

    void update(Long id, RoleUpdateDTO dto);

    void delete(Long id);

    void assignPermissions(Long id, RolePermissionAssignDTO dto);
}
