package com.bdis.modules.user.service;

import com.bdis.modules.user.dto.DepartmentDTO;
import com.bdis.modules.user.query.DepartmentQuery;
import com.bdis.modules.user.vo.DepartmentVO;
import java.util.List;

public interface DepartmentService {

    List<DepartmentVO> tree(DepartmentQuery query);

    DepartmentVO detail(Long id);

    Long create(DepartmentDTO dto);

    void update(Long id, DepartmentDTO dto);

    void delete(Long id);
}
