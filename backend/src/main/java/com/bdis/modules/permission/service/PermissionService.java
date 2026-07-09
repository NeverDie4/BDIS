package com.bdis.modules.permission.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.permission.dto.PermissionDTO;
import com.bdis.modules.permission.query.PermissionQuery;
import com.bdis.modules.permission.vo.PermissionVO;

public interface PermissionService {

    PageResult<PermissionVO> page(PermissionQuery query);

    PermissionVO detail(Long id);

    Long create(PermissionDTO dto);

    void update(Long id, PermissionDTO dto);

    void delete(Long id);
}
