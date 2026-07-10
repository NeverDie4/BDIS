package com.bdis.modules.user.service;

import com.bdis.common.core.PageResult;
import com.bdis.modules.user.dto.OrganizationDTO;
import com.bdis.modules.user.query.OrganizationQuery;
import com.bdis.modules.user.vo.OrganizationVO;

public interface OrganizationService {

    PageResult<OrganizationVO> page(OrganizationQuery query);

    OrganizationVO detail(Long id);

    Long create(OrganizationDTO dto);

    void update(Long id, OrganizationDTO dto);

    void delete(Long id);
}
