package com.bdis.modules.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.bdis.modules.permission.dto.DataScopeDTO;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.permission.vo.DataScopeVO;
import java.util.List;

public interface DataScopeService {

    DataScopeResultVO resolveForCurrentUser(String resourceType);

    <T> DataScopeResultVO applyToQuery(
            LambdaQueryWrapper<T> wrapper,
            String resourceType,
            SFunction<T, ?> ownerField,
            SFunction<T, ?> organizationField,
            SFunction<T, ?> departmentField);

    List<DataScopeVO> listByRole(Long roleId);

    Long save(DataScopeDTO dto);

    void delete(Long id);
}
