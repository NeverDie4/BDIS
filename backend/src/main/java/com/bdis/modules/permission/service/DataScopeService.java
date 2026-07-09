package com.bdis.modules.permission.service;

import com.bdis.modules.permission.dto.DataScopeDTO;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.permission.vo.DataScopeVO;
import java.util.List;

public interface DataScopeService {

    DataScopeResultVO resolveForCurrentUser(String resourceType);

    List<DataScopeVO> listByRole(Long roleId);

    Long save(DataScopeDTO dto);

    void delete(Long id);
}
